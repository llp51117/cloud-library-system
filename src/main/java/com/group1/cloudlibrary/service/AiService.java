package com.group1.cloudlibrary.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import com.group1.cloudlibrary.entity.Book;
import com.group1.cloudlibrary.mapper.BookMapper;
import com.group1.cloudlibrary.util.UserContext;
import com.group1.cloudlibrary.vo.ChatVO;
import com.group1.cloudlibrary.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI 能力服务：智能问答、语义向量、AI 摘要、AI 分析报告
 * <p>
 * 当 {@code cloud-library.ai.enabled=true} 且配置了 API Key 时，调用智谱 GLM 的 OpenAI 兼容接口
 * （POST {chat-url}，请求体为 messages 数组，取 choices[0].message.content）；
 * 否则自动降级为本地知识库规则回答，保证系统可离线运行。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Authorization 头前缀，必须和 Key 之间留一个空格 */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 请求地址「写死」：只调智谱 GLM 的 OpenAI 兼容接口，
     * 不再从配置文件读取、也不做任何阿里云风格的路径拼接（没有 /apps/、没有 /completion）。
     */
    private static final String CHAT_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    /** 智谱默认模型（application.yml 未配置 model 时兜底使用）：glm-4-flash，免费且更稳定 */
    private static final String DEFAULT_MODEL = "glm-4-flash";

    /** 连接超时：10 秒 */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** 读取超时：60 秒，给大模型足够时间把话说完（默认超时会 Read timed out） */
    private static final int READ_TIMEOUT_MS = 60_000;

    /**
     * 对话专用的 RestTemplate：显式配置超时，不用默认（默认无超时 / 过短都会出问题）。
     * 大模型要逐字生成回答，读超时必须给足 60 秒。
     */
    private static final RestTemplate CHAT_REST_TEMPLATE = createChatRestTemplate();

    private static RestTemplate createChatRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

    /** 「查总数」类问法的标志词 */
    private static final List<String> COUNT_WORDS = List.of(
            "多少", "总数", "总数量", "一共", "总共", "数量", "几本", "馆藏量", "有多少");

    /** 命中这些词说明在问借阅/预约规则，不查库（规则由 borrowRules() 提供） */
    private static final List<String> RULE_QUESTION_WORDS = List.of(
            "规则", "多久", "几天", "期限", "逾期", "超期", "过期", "续借", "预约", "排队", "罚款", "滞纳金",
            "赔偿", "遗失", "丢失", "上限", "最多", "怎么借", "如何借", "怎么还", "如何还", "借书", "还书",
            "借不了", "还不了");

    /** 打招呼用语：这类输入纯聊天，不查库 */
    private static final List<String> GREETING_WORDS = List.of(
            "你好", "您好", "哈喽", "嗨", "早上好", "中午好", "晚上好", "在吗", "在么", "hi", "hello", "hey");

    /** 追问上一本书的问法：这类问题本轮没有核心词，需要沿用上一轮提问里的词 */
    private static final List<String> FOLLOW_UP_WORDS = List.of(
            "相关", "类似", "同类", "同类型", "差不多", "还有", "别的", "其他", "其它", "再推荐", "同款", "这种");

    /** 条件提取时要剔除的废话：疑问词、量词、礼貌用语等 */
    private static final List<String> QUERY_NOISE_WORDS = List.of(
            "请问", "帮我", "麻烦", "我想", "我要", "我想看", "有没有", "有哪些", "有什么", "哪些", "什么",
            "多少", "几本", "总数", "总数量", "一共", "总共", "数量", "馆藏量", "有", "没有", "推荐", "想看",
            "好看", "搜索", "查询", "查", "找一下", "找找", "找", "看看", "一下", "给我", "请", "的", "了",
            "是", "在", "吗", "呢", "吧", "啊", "和", "与", "或者", "还有", "其他", "另外", "这里", "那里",
            "馆藏", "图书馆", "系统", "你们", "咱们", "我们", "大家", "里面", "里面还", "现在", "目前");

    /** 条件提取后剩下的这些泛词不算条件（同一个词反复去掉尾部后缀） */
    private static final List<String> GENERIC_BOOK_WORDS = List.of(
            "图书", "书籍", "小说", "读物", "书单", "作品", "书", "本", "部", "个", "些");

    /** 没有实际问题的输入：笑声、语气词、叠词等 */
    private static final Pattern CASUAL_PATTERN = Pattern.compile(
            "^(?:哈|呵|嘿|嘻|嗝|吼|哦|啊|嗯|唔|诶|额|欸|哎|唉|em+|hh+|66+|88+|滴|哒|嘤|呜|哇|"
                    + "呀|嘛|吧|呢|了|的|是|好|行|ok|okk|hi|hello|hey|在吗|在么|test|测试)+$",
            Pattern.CASE_INSENSITIVE);

    /** 标点、空白、数字等噪音字符，用于判断输入是否还有实质内容 */
    private static final Pattern NOISE_PATTERN =
            Pattern.compile("[\\p{Punct}\\s\\d，。！？、；：\"”‘“'（）【】《》~～·\\-_*^]");

    /**
     * 人设（灵枢）：所有智能问答的 prompt 都会在最开头强制带上这一段，
     * 保证智能体像真人管理员一样说话，而不是念数据库。
     */
    private static final String PERSONA = """
            【你的人设（必须严格遵守，优先级最高）】
            你是云图书管理系统的图书管理员「灵枢」，一个真正在柜台后工作的真人同事。
            1. 说话要像真人一样自然：先顺着用户的话简短回应一句，再推荐书或解释规则；
               不要用「您好！根据您提供的信息」「首先/其次/最后」这类客服腔和官腔开头；
            2. 绝对禁止输出任何形式的代码、JSON、表格、Markdown 语法、编号清单、方括号或书名号堆叠的列表；
               推荐图书时用一两句自然的话讲出来，像在柜台后聊天一样；
            3. 绝对不要说「系统实时数据」「数据库查询结果」「根据检索结果」「符合条件的有X本」这类机器人的话；
               下面给你的馆藏信息请当成你自己脑子里记住的书，用口语说出来；
            4. 没找到书时坦诚说明情况，再自然地把下面给出的热门书推荐给他，语气真诚、不敷衍；
            5. 用中文回答，像微信聊天一样自然，150~250 字之间，除非用户要求详细否则不要更长。
            """;

    /**
     * 数据使用说明：告诉大模型什么时候用实时馆藏数据、什么时候忽略它。
     * 「你好」这类闲聊必须直接聊天，绝口不提数据。
     */
    private static final String CHAT_GUIDE = """
            【怎么使用「实时馆藏数据」】
            1. 用户在找书、问某本书、问有没有某类书：结合实时馆藏数据回答，只说数据里有的书，一本都不要编造；
            2. 用户只是闲聊或打招呼（如「你好」「哈哈」「在吗」）：直接像朋友一样回一句，
               完全不要提数据，绝对不要说「系统实时数据」「查询到 0 本」这类话；
            3. 数据显示「馆藏没有查到相关图书」：**绝对禁止说「去首页搜索」「在首页试试全量搜索」「说不定会有惊喜」
               这类客服套话**。必须坦诚地共情并说明：馆里暂时没找到跟他说的特别相关的书，
               因为系统目前没有细分书籍类型，所以没法精准分类推荐；然后自然地把下面给出的热门书推荐给他。
               参考说法：「馆里暂时没找到跟这个特别相关的书。因为系统目前没有细分书籍类型，
               所以我没法精准分类推荐。要不你看看这几本最近大家爱看的书？」
               （不要照抄，用自己的话自然地说，也不要反问「你想看什么类型」来敷衍）；
            4. 数据显示「简介里提到了相关内容」：说明这几本书跟他要找的内容有关联，
               自然地讲出来，不要说「数据库里查到」这类话；
            5. 用户问借阅 / 续借 / 预约规则：按下面给出的系统业务规则回答，不要编造天数。
            """;

    /**
     * 系统内置的借阅规则（不编造天数）
     * <p>天数全部取自 application.yml 的 cloud-library.borrow.* 与 reservation.keep-hours，
     * 与真实业务逻辑同源；改配置后 AI 的回答自动跟着变，不会再出现「配置改了、AI 还按旧数字回答」。</p>
     */
    private String borrowRules() {
        CloudLibraryProperties.Borrow borrow = properties.getBorrow();
        return "1. 借阅期限：每本图书借阅期 " + borrow.getDays() + " 天，到期前 " + borrow.getRenewBeforeDays()
                + " 天内可续借 " + borrow.getRenewLimit() + " 次，续借可再延长 " + borrow.getRenewDays() + " 天。\n"
                + "2. 预约规则：图书被借出后点击「预约 / 借阅」即可排队；图书归还后，排在第一位的读者会收到通知，"
                + "并需在 " + properties.getReservation().getKeepHours()
                + " 小时内完成借阅，超时自动顺延给下一位。";
    }

    /**
     * 系统提示词里的「真实业务规则」：只写系统里真实存在的规则，重点堵住两类幻觉——
     * 「最多能借 N 本」和「逾期要罚款 / 扣信用分」。
     * <p>天数类数字仍取自 {@link #borrowRules()}（application.yml），保证与代码同源。</p>
     */
    private String serviceRules() {
        return """
                【真实业务规则（严禁编造，答错即为严重错误）】
                """ + borrowRules() + "\n" + """
                3. **系统没有「最多可借 N 本」的数量限制**：用户能借多少本只受图书库存限制。
                   任何人问「最多能借几本 / 能借多少本」，都必须回答「没有数量上限，只要库存有书就可以借」，
                   **绝对禁止编造「最多借 5 本」「每人限借 3 本」之类的数字**。
                4. **没有任何罚款、滞纳金、赔偿金、逾期次数限制或信用分机制**：
                   逾期只会被标记「已逾期」状态并收到催还 / 逾期通知，**不会罚款**。
                5. 逾期判定：应还时间早于当前时间、记录仍为「借阅中」时自动标记「已逾期」；
                   应还时间精确到分钟（yyyy-MM-dd HH:mm），从借阅时刻起算。
                6. 借阅前提：账号状态正常（未注销、未被禁用），同一本图书不能重复借阅；
                   库存 > 0 才能借，借出后库存减 1、归还后加 1；库存为 0 时提示「该图书暂无库存，请预约」。
                7. 到号保护：某书仅剩最后 1 本且已有他人处于「可借阅」预约状态时，点借阅会被自动放入预约队列并提示排队位次。
                8. 归还：由用户在用户端自行归还，归还后通知本人并自动唤醒下一位预约者；已归还的图书不能续借。
                9. 续借：在原应还时间基础上延长（不是从今天起算），每本最多续借 1 次；
                   只能在应还日前 3 天内办理；有人排队预约时不允许续借。
                10. 预约：只有库存为 0 时才能预约，库存 > 0 时提示「当前有库存，可直接借阅，无需预约」；
                    按提交时间先后排队；归还后第一位变为「可借阅」，保留 24 小时，超时自动取消并顺延给下一位。
                11. 催还：管理员一键催还与每天 09:00 自动催还，覆盖「已逾期」以及「3 天内即将到期」的记录。
                12. 权限：普通用户可借书；管理员（role=1）不能借书、不能预约；待激活管理员只能还书；最高管理员拥有全部权限。
                13. 上面没写到的规则（如注册登录、封号、审批等）如果用户问到，只说「这块我这边查不到确切规则，建议在对应页面查看或联系管理员」，不要编造。
                """;
    }

    /**
     * 智能体不可用（没配 Key / 调用失败 / 返回空）时的唯一兜底回复。
     * <p>只要走降级就只回这一句：自然口语，绝不暴露数据库原文。</p>
     */
    private static final String AI_UNAVAILABLE_REPLY = "抱歉，我这边刚刚卡了一下，没能好好回答你。麻烦你稍等几秒再问一次，谢谢理解～";

    /** 多轮对话：每个用户最多保留最近几轮（防止 prompt 越来越长、内存无限增长） */
    private static final int MAX_HISTORY_ROUNDS = 5;

    /**
     * 每个用户的聊天历史：key = 用户ID（未登录时为 anonymous），value = 最近 N 轮对话。
     * 每条记录的格式：用户问：xxx；你答：yyy
     * <p>只存在内存里，进程重启即清空；用于让智能体理解「第一本有库存吗」这类带指代的问题。</p>
     */
    private final ConcurrentHashMap<String, List<String>> chatHistories = new ConcurrentHashMap<>();

    /** 图书向量缓存：key = 图书ID，避免每次语义搜索都对全表逐本调用 Embedding 接口（2026-10-26 新增） */
    private final ConcurrentHashMap<Long, float[]> bookVectorCache = new ConcurrentHashMap<>();

    private final CloudLibraryProperties properties;
    private final RestTemplate restTemplate;
    private final BookMapper bookMapper;

    // ==========================================================
    // 1. 智能问答（RAG：先检索知识库，再交给大模型生成）
    // ==========================================================

    /**
     * 智能问答入口：负责多轮上下文的读写（取历史 → 回答 → 记历史），
     * 具体的判断与生成逻辑见 {@link # doChat(String, String)}。
     */
    public ChatVO chat(String question) {
        String text = question == null ? "" : question.trim();
        String historyKey = historyKey();
        // 先把这位用户之前说过的话拼出来，智能体才知道「第一本」指的是哪本
        String historyText = buildHistoryText(historyKey);

        ChatVO vo = new ChatVO();
        vo.setReferences(new ArrayList<>());

        // 【第一步】不管用户问的是「你好」「借阅规则」还是「推荐书」，一律先发给 GLM 大模型，
        //          由大模型自己判断该聊天、讲规则还是推荐书，Java 侧不再用关键词硬匹配分流。
        if (isAiEnabled()) {
            try {
                // 【第二步】先用用户原话去库里模糊查一次，结果只作为给大模型的「参考资料」塞进 prompt，
                //          绝不直接展示给用户；查不到就告诉大模型「没查到，请引导去首页搜索」。
                String reference = buildReferenceData(text, historyKey);
                // system = 人设 + 数据使用说明；user = 规则 + 历史 + 问题 + 查库参考资料
                String answer = callChatModel(buildChatSystemPrompt(),
                        buildChatPrompt(text, historyText, reference));
                if (StringUtils.hasText(answer)) {
                    vo.setAnswer(answer);
                    vo.setAiGenerated(true);
                    // 拿到回答后把这一轮记进历史（超过 5 轮就丢掉最早的一轮）
                    appendHistory(historyKey, text, answer);
                    return vo;
                }
                log.error("智谱没有返回任何内容，本次走友好兜底（参考资料={}）", reference);
            } catch (Exception e) {
                log.error("智谱调用失败，本次走友好兜底：{}，异常堆栈如下：", e.getMessage(), e);
            }
        }

        // 【第三步】任何异常 / 空回复都不暴露数据库原始数据，只回一句亲切的话
        vo.setAnswer(AI_UNAVAILABLE_REPLY);
        vo.setAiGenerated(false);
        return vo;
    }

    /**
     * 清空某个用户的多轮对话历史（前端点「清空对话」时调用）
     *
     * @param userId 用户唯一标识，取 {@link #historyKey()} 的结果；传 null 时按当前登录用户处理
     */
    public void clearHistory(String userId) {
        String key = StringUtils.hasText(userId) ? userId : historyKey();
        chatHistories.remove(key);
        log.info("已清空对话历史：key={}", key);
    }

    /**
     * system 消息（messages 里 role=system 的 content）：
     * 人设（灵枢） → 数据使用说明 → 真实业务规则
     */
    private String buildChatSystemPrompt() {
        return PERSONA + "\n" + CHAT_GUIDE + "\n" + serviceRules();
    }

    /**
     * 组装发给 GLM 的 user 消息：多轮历史 → 用户问题 → 实时馆藏参考资料
     */
    private String buildChatPrompt(String text, String historyText, String reference) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(historyText)) {
            sb.append(historyText).append("\n");
        }
        sb.append("用户问题：").append(StringUtils.hasText(text) ? text : "（用户没有输入任何内容）").append("。\n");
        sb.append("【系统为你准备的实时馆藏数据（只有你能看到，仅供参考）】\n")
                .append(StringUtils.hasText(reference) ? reference : "（本次没有查询书库）");
        return sb.toString();
    }

    /**
     * 给大模型准备「查库工具」的结果，最多取 5 本。查询顺序：
     * <ol>
     *   <li>书名 / 作者 / 平台 / 简介 关键词 LIKE（现有 searchByKeywords）；</li>
     *   <li>上面没命中 → 用核心词去 description 里再 LIKE 一次（相似推荐）；</li>
     *   <li>还是没有 → 坦诚说明 + 返回热门书兜底。</li>
     * </ol>
     * <p>注意：这里的文字只进 Prompt，永远不会直接返回前端。</p>
     */
    private String buildReferenceData(String text, String key) {
        // 纯闲聊（「你好」「哈哈」「？？？」）：不查库，Prompt 里也不带数据，让模型像朋友一样聊天
        if (!StringUtils.hasText(text) || isCasualInput(text)) {
            return null;
        }
        // 借阅 / 预约规则咨询：规则已经在 Prompt 里，没必要查书库
        if (RULE_QUESTION_WORDS.stream().anyMatch(text::contains)) {
            return null;
        }
        // 打招呼（「你好」之类）：纯聊天，查库只会得到 0 条，直接不带数据
        String lower = text.toLowerCase();
        if (GREETING_WORDS.stream().anyMatch(lower::contains)) {
            return null;
        }
        boolean askCount = COUNT_WORDS.stream().anyMatch(text::contains);
        // 本轮没提取到核心词、又是在追问上一本（「还有类似的吗」「有没有相关的」）时，沿用上一轮的核心词
        List<String> keywords = extractConditionKeywords(text);
        if (keywords.isEmpty() && isFollowUpQuestion(text)) {
            keywords = lastQuestionKeywords(key);
        }
        try {
            // 只问总数（如「一共有多少本书」）
            if (askCount && keywords.isEmpty()) {
                Long total = bookMapper.selectCount(null);
                return "馆藏图书总数：" + (total == null ? 0 : total) + " 本。";
            }
            if (keywords.isEmpty()) {
                // 用户没说具体书名（如「推荐本好看的书」）：给一批受欢迎的书当参考
                List<Book> popular = bookMapper.selectPopularBooks(5);
                if (popular.isEmpty()) {
                    return "书库里暂时还没有图书。";
                }
                return "用户没有说具体书名，以下是书库里比较受欢迎的书：" + formatBooks(popular, false);
            }
            // 第一查：书名 / 作者等字段
            long count = bookMapper.countByKeywords(keywords);
            if (count > 0) {
                List<Book> books = bookMapper.searchByKeywords(keywords, 5);
                return "书库里查到 " + count + " 本相关的书，最多列出 5 本：" + formatBooks(books, false);
            }
            // 第二查：书名 / 作者没命中，拿核心词去简介里模糊查一次（如「刘慈欣」只在简介里出现）
            List<Book> byDescription = bookMapper.searchByDescription(keywords, 5);
            if (!byDescription.isEmpty()) {
                return "书名和作者里没有完全匹配的，但简介里提到了相关内容，这几本可能合他心意："
                        + formatBooks(byDescription, true);
            }
            // 都没查到：坦诚说明 + 热门书兜底
            return buildNotFoundReference();
        } catch (Exception e) {
            // 查库失败不影响问答本身：不带参考资料，让模型自由回答
            log.warn("馆藏资料查询失败（本次不带参考资料）：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 什么都没查到时给大模型的参考资料：明确禁止「去首页搜索」这类客服套话，
     * 要求它坦诚共情，然后用热门书兜底推荐。
     */
    private String buildNotFoundReference() {
        List<Book> popular;
        try {
            popular = bookMapper.selectPopularBooks(3);
        } catch (Exception e) {
            popular = List.of();
        }
        StringBuilder sb = new StringBuilder("馆藏没有查到相关图书。")
                .append("请坦诚地告诉用户：馆里暂时没找到跟这个特别相关的书，因为系统目前没有细分书籍类型，")
                .append("所以没法精准分类推荐；然后自然地推荐下面这几本最近大家爱看的书，语气真诚，不要说套话。");
        if (popular.isEmpty()) {
            sb.append("（书库暂时还没有可推荐的书）");
            return sb.toString();
        }
        sb.append("最近大家爱看的书：").append(formatBooks(popular, false));
        return sb.toString();
    }

    /** 把图书列表拼成一行行「书名 / 作者 / 库存」；withDescription=true 时额外带一小段简介 */
    private String formatBooks(List<Book> books, boolean withDescription) {
        StringBuilder sb = new StringBuilder();
        for (Book book : books) {
            sb.append("书名：").append(nvl(book.getTitle()))
                    .append("，作者：").append(nvl(book.getAuthor()))
                    .append("，库存：").append(book.getStock() == null ? 0 : book.getStock()).append("本");
            if (withDescription && StringUtils.hasText(book.getDescription())) {
                sb.append("，简介：").append(trim(book.getDescription(), 60));
            }
            sb.append("；");
        }
        return sb.toString();
    }

    /** 判断是不是在追问上一本：「还有类似的吗」「有没有相关的」这类 */
    private boolean isFollowUpQuestion(String text) {
        return FOLLOW_UP_WORDS.stream().anyMatch(text::contains);
    }

    /** 取上一轮提问里的核心词（用于追问场景），取不到返回空列表 */
    private List<String> lastQuestionKeywords(String key) {
        List<String> history = chatHistories.get(key);
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        String last = history.get(history.size() - 1);
        int start = last.indexOf("用户之前问过：");
        int end = last.indexOf("；你回答过：");
        if (start < 0 || end <= start) {
            return List.of();
        }
        return extractConditionKeywords(last.substring(start + "用户之前问过：".length(), end));
    }

    /** 空值兜底，避免在 Prompt 里出现 null */
    private String nvl(String value) {
        return value == null ? "未知" : value;
    }

    /**
     * 判断问题是不是「没有实际问题」：空、纯标点、笑声、语气词等
     */
    private boolean isCasualInput(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        // 去掉标点、空白、数字后如果没有内容，说明是纯符号（如「？？？」「...」）
        String stripped = NOISE_PATTERN.matcher(text).replaceAll("");
        if (!StringUtils.hasText(stripped)) {
            return true;
        }
        return CASUAL_PATTERN.matcher(stripped).matches();
    }



    /**
     * 从提问里提取真正用于查询的条件词（书名 / 作者 / 题材）
     * <p>例：「有哪些科幻书」→ [科幻]；「刘慈欣的书」→ [刘慈欣]；「一共有多少本书」→ []（无具体条件）</p>
     */
    private List<String> extractConditionKeywords(String text) {
        // 只保留中日韩、英文、数字，其余标点当分隔符
        String cleaned = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ");
        for (String noise : QUERY_NOISE_WORDS) {
            cleaned = cleaned.replace(noise, " ");
        }
        List<String> keywords = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) {
            String word = stripGenericSuffix(token);
            if (word.length() >= 2 && !GENERIC_BOOK_WORDS.contains(word) && !keywords.contains(word)) {
                keywords.add(word);
            }
        }
        // 条件过多会让 LIKE 命中全表，最多取 3 个
        return keywords.size() > 3 ? keywords.subList(0, 3) : keywords;
    }

    /** 去掉「书 / 图书 / 小说」这类泛化后缀，让「科幻书」变成可检索的「科幻」 */
    private String stripGenericSuffix(String token) {
        String word = token;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String generic : GENERIC_BOOK_WORDS) {
                if (word.length() > generic.length() && word.endsWith(generic)) {
                    word = word.substring(0, word.length() - generic.length());
                    changed = true;
                }
            }
        }
        return word;
    }

    // ==========================================================
    // 多轮对话历史（内存存储，最近 5 轮）
    // ==========================================================

    /** 历史记录的 key：登录用户用 userId，未登录时用 anonymous（/api/ai 需要登录，兜底仅为安全） */
    private String historyKey() {
        Long userId = UserContext.getUserId();
        return userId == null ? "anonymous" : String.valueOf(userId);
    }

    /**
     * 把一个用户的历史记录拼成一段文本，供智能体理解上下文指代（如「第一本」）
     */
    private String buildHistoryText(String key) {
        List<String> history = chatHistories.get(key);
        if (history == null || history.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("以下是你与该用户之前的对话，仅用于理解上下文（如「第一本」「它」这类指代），不要复述这些内容：");
        for (String round : history) {
            sb.append("\n").append(round);
        }
        return sb.toString();
    }

    /** 把这一轮问答追加进历史，超过 5 轮就移除最早的一轮 */
    private void appendHistory(String key, String question, String answer) {
        List<String> history = chatHistories.computeIfAbsent(key, k -> new java.util.ArrayList<>());
        history.add("用户之前问过：" + trim(question, 100) + "；你回答过：" + trim(answer, 200));
        while (history.size() > MAX_HISTORY_ROUNDS) {
            history.remove(0);
        }
    }

    // ==========================================================
    // 2. 语义搜索（Embedding 向量相似度）
    // ==========================================================

    /**
     * 语义搜索：返回按语义相似度排序的图书
     * <p>启用 Embedding 时使用向量余弦相似度；否则自动降级为关键词语义检索。</p>
     */
    public List<Book> semanticSearch(String query, int limit) {
        List<Book> candidates = bookMapper.selectList(null);
        if (candidates.isEmpty()) {
            return candidates;
        }

        if (isAiEnabled()) {
            try {
                float[] queryVector = embed(query);
                if (queryVector != null) {
                    List<Book> scored = new ArrayList<>();
                    for (Book book : candidates) {
                        float[] bookVector = embedCached(book);
                        if (bookVector == null) {
                            continue;
                        }
                        book.setScore(cosine(queryVector, bookVector));
                        scored.add(book);
                    }
                    if (!scored.isEmpty()) {
                        return scored.stream()
                                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                                .limit(limit)
                                .collect(Collectors.toList());
                    }
                }
            } catch (Exception e) {
                log.warn("语义向量检索失败，降级为关键词检索：{}", e.getMessage());
            }
        }

        // 降级：关键词语义检索
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return candidates.stream().limit(limit).collect(Collectors.toList());
        }
        return bookMapper.searchByKeywords(keywords, limit);
    }

    /**
     * 文本向量化（调用 Embedding 接口），失败返回 null
     */
    public float[] embed(String text) {
        if (!isAiEnabled() || !StringUtils.hasText(text)) {
            return null;
        }
        // DeepSeek 等平台不提供 embeddings 接口：未单独配置向量地址时直接跳过，
        // 避免对每本书都发出必然失败的 HTTP 请求，语义搜索自动降级为关键词检索
        String embeddingBaseUrl = properties.getAi().getEmbeddingBaseUrl();
        if (!StringUtils.hasText(embeddingBaseUrl) || !StringUtils.hasText(properties.getAi().getEmbeddingModel())) {
            return null;
        }
        try {
            String url = embeddingBaseUrl + "/embeddings";
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", properties.getAi().getEmbeddingModel());
            body.put("input", trim(text, 1000));

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), jsonHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            JsonNode root = MAPPER.readTree(response.getBody());
            JsonNode embedding = root.path("data").path(0).path("embedding");
            if (!embedding.isArray()) {
                return null;
            }
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.warn("Embedding 调用失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 图书向量（带缓存）：每本书只调一次 Embedding 接口，结果缓存后重复使用；
     * 首次语义搜索会全表算一遍（约 26 本 ≈ 15 秒），之后每次搜索只算查询向量（约 1 秒）。
     */
    private float[] embedCached(Book book) {
        if (book == null || book.getId() == null) {
            return null;
        }
        float[] cached = bookVectorCache.get(book.getId());
        if (cached != null) {
            return cached;
        }
        String text = nvl(book.getTitle()) + " " + nvl(book.getAuthor()) + " "
                + (book.getPlatform() == null ? "" : book.getPlatform()) + " "
                + trim(book.getDescription(), 200);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        float[] vector = embed(text);
        if (vector != null) {
            bookVectorCache.put(book.getId(), vector);
        }
        return vector;
    }

    // ==========================================================
    // 3. AI 摘要（图书编目辅助）
    // ==========================================================

    public String generateBookSummary(String title, String author, String description) {
        if (isAiEnabled()) {
            try {
                String prompt = """
                        请为下面这本图书生成一段 80 字以内的中文简介摘要，用于图书管理系统的编目展示。
                        书名：%s
                        作者：%s
                        已有信息：%s
                        只输出摘要正文，不要任何多余说明。
                        """.formatted(title, author, StringUtils.hasText(description) ? description : "无");
                String answer = callChatModel(prompt);
                if (StringUtils.hasText(answer)) {
                    return answer.trim();
                }
            } catch (Exception e) {
                log.warn("AI 摘要生成失败，使用本地摘要：{}", e.getMessage());
            }
        }
        return "《" + title + "》由" + author + "创作，"
                + (StringUtils.hasText(description) ? trim(description, 60) : "是一部值得一读的优质作品")
                + "。适合喜欢该题材的读者阅读收藏。（本地自动生成摘要）";
    }

    // ==========================================================
    // 4. AI 分析报告（数据概览）
    // ==========================================================

    public String generateAnalysisReport(StatisticsVO statistics, List<java.util.Map<String, Object>> hotBooks,
                                         List<java.util.Map<String, Object>> trend) {
        String dataText = buildStatisticsText(statistics, hotBooks, trend);
        if (isAiEnabled()) {
            try {
                String prompt = """
                        你是图书馆数据分析师，请根据下面的系统运营数据，生成一份 300 字以内的中文分析报告，
                        内容包含：整体情况总结、热门图书解读、借阅趋势观察、运营改进建议。
                        请使用分点结构，语言专业简洁。

                        【运营数据】
                        %s
                        """.formatted(dataText);
                String answer = callChatModel(prompt);
                if (StringUtils.hasText(answer)) {
                    return answer.trim();
                }
            } catch (Exception e) {
                log.warn("AI 分析报告生成失败，使用本地报告：{}", e.getMessage());
            }
        }
        return buildLocalReport(statistics, hotBooks, trend, dataText);
    }

    private String buildStatisticsText(StatisticsVO stats, List<java.util.Map<String, Object>> hotBooks,
                                       List<java.util.Map<String, Object>> trend) {
        StringBuilder sb = new StringBuilder();
        sb.append("总藏书量：").append(stats.getTotalBooks()).append(" 本\n");
        sb.append("总用户数：").append(stats.getTotalUsers()).append(" 人\n");
        sb.append("当前借阅中：").append(stats.getBorrowingCount()).append(" 本\n");
        sb.append("逾期未还：").append(stats.getOverdueCount()).append(" 本\n");
        if (hotBooks != null && !hotBooks.isEmpty()) {
            sb.append("热门图书 TOP5：");
            sb.append(hotBooks.stream()
                    .map(m -> String.valueOf(m.get("bookTitle")) + "(" + m.get("value") + "次)")
                    .collect(Collectors.joining("、")));
            sb.append("\n");
        }
        if (trend != null && !trend.isEmpty()) {
            sb.append("近期借阅趋势：");
            sb.append(trend.stream()
                    .map(m -> String.valueOf(m.get("day")) + " 借出 " + m.get("count") + " 本")
                    .collect(Collectors.joining("；")));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildLocalReport(StatisticsVO stats, List<java.util.Map<String, Object>> hotBooks,
                                    List<java.util.Map<String, Object>> trend, String dataText) {
        StringBuilder sb = new StringBuilder();
        sb.append("【一、整体情况】\n");
        sb.append("当前系统共有藏书 ").append(stats.getTotalBooks()).append(" 本，注册用户 ")
                .append(stats.getTotalUsers()).append(" 人，借阅中 ").append(stats.getBorrowingCount())
                .append(" 本，逾期 ").append(stats.getOverdueCount()).append(" 本，整体运行平稳。\n\n");
        sb.append("【二、热门图书】\n");
        if (hotBooks == null || hotBooks.isEmpty()) {
            sb.append("暂无借阅排行数据。\n");
        } else {
            for (int i = 0; i < hotBooks.size(); i++) {
                java.util.Map<String, Object> m = hotBooks.get(i);
                sb.append(i + 1).append(". 《").append(m.get("bookTitle")).append("》 借阅 ")
                        .append(m.get("value")).append(" 次\n");
            }
        }
        sb.append("\n【三、借阅趋势】\n");
        if (trend == null || trend.isEmpty()) {
            sb.append("暂无趋势数据。\n");
        } else {
            int total = trend.stream().mapToInt(m -> Integer.parseInt(String.valueOf(m.get("count")))).sum();
            sb.append("统计周期内共产生借阅 ").append(total).append(" 次，日均约 ")
                    .append(trend.isEmpty() ? 0 : total / trend.size()).append(" 次。\n");
        }
        sb.append("\n【四、运营建议】\n");
        if (stats.getOverdueCount() != null && stats.getOverdueCount() > 0) {
            sb.append("1. 当前存在逾期图书，建议通过通知公告与催还提醒督促读者及时归还；\n");
        }
        sb.append("2. 可结合热门图书排行补充同类馆藏，提升图书周转率；\n");
        sb.append("3. 建议定期发布新书上架公告，提升用户活跃度。\n");
        sb.append("\n（本报告由本地规则引擎生成，未接入大模型）");
        return sb.toString();
    }

    // ==========================================================
    // 工具方法
    // ==========================================================

    /**
     * 调用智谱 GLM 接口（不带系统提示）
     */
    private String callChatModel(String prompt) {
        return callChatModel(null, prompt);
    }

    /**
     * 调用智谱 GLM（OpenAI 兼容接口）：
     * POST {chat-url}
     * <p>
     * 请求体：{"model":"glm-4.7-flash","messages":[{"role":"system","content":"..."},{"role":"user","content":"..."}]}
     * 返回 JSON 取 choices[0].message.content 作为回答。
     * </p>
     */
    private String callChatModel(String systemPrompt, String prompt) {
        String apiKey = properties.getAi().getApiKey();
        if (!StringUtils.hasText(apiKey)) {
            log.error("cloud-library.ai.api-key 读到空值，无法调用智谱接口；"
                    + "请检查 application.yml 中 cloud-library.ai.api-key 是否配置正确");
            return null;
        }
        // Key 只允许字母数字和 . _ - ：出现空格、中文弯引号等字符说明配置文件被污染，会直接 401
        if (!apiKey.trim().matches("[A-Za-z0-9._\\-]+")) {
            log.error("cloud-library.ai.api-key 含有非法字符（空格 / 中文引号等），会导致 401：{}", mask(apiKey));
        }
        try {
            String model = StringUtils.hasText(properties.getAi().getModel())
                    ? properties.getAi().getModel() : DEFAULT_MODEL;

            // OpenAI 兼容格式：{"model":"glm-4.7-flash","messages":[{"role":"system",...},{"role":"user",...}]}
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            com.fasterxml.jackson.databind.node.ArrayNode messages = body.putArray("messages");
            if (StringUtils.hasText(systemPrompt)) {
                ObjectNode systemMessage = messages.addObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
            }
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            Double temperature = properties.getAi().getTemperature();
            body.put("temperature", temperature == null ? 0.7 : temperature);

            // 请求头：Content-Type: application/json + Authorization: Bearer <智谱 API_KEY>
            HttpHeaders headers = jsonHeaders();
            log.info("调用智谱 GLM：url={}, model={}, connectTimeout={}ms, readTimeout={}ms, "
                            + "Content-Type={}, Authorization={}",
                    CHAT_URL, model, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS,
                    headers.getFirst(HttpHeaders.CONTENT_TYPE), maskHeader(headers));
            log.info("发送给智谱的 messages：{}", body.toString());
            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            // 用带超时配置的 RestTemplate（连接 10s / 读取 60s）执行请求
            ResponseEntity<String> response = CHAT_REST_TEMPLATE.postForEntity(CHAT_URL, entity, String.class);

            String responseBody = response.getBody();
            log.info("智谱原始返回：HTTP {}，body={}", response.getStatusCode().value(), responseBody);
            JsonNode root = MAPPER.readTree(responseBody);

            // 智谱出错时返回 {"error":{"code":"...","message":"..."}}
            JsonNode error = root.path("error");
            if (!error.isMissingNode() && StringUtils.hasText(error.path("message").asText())) {
                log.error("智谱接口返回错误：code={}，message={}，完整响应={}",
                        error.path("code").asText(), error.path("message").asText(), responseBody);
                return null;
            }

            // 取 choices[0].message.content
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && StringUtils.hasText(content.asText())) {
                return content.asText().trim();
            }
            log.warn("智谱返回内容中没有 choices[0].message.content：{}", responseBody);
            return null;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 读超时 / 连接失败：大模型生成慢时会 Read timed out，调大 cloud-library.ai.timeout-seconds 即可
            log.error("智谱请求超时或网络异常（可调大 cloud-library.ai.timeout-seconds）：{}，异常堆栈如下：",
                    e.getMessage(), e);
            return null;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            // 4xx / 5xx（如 401 鉴权失败、429 限流）：响应体 + 堆栈都打出来
            log.error("智谱接口调用失败(已降级)：HTTP {}，响应体={}，异常堆栈如下：",
                    e.getStatusCode().value(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("智谱接口调用失败(已降级)：{}，异常堆栈如下：", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 通用 JSON 请求头：Content-Type / Accept 固定，Authorization 必须是 Bearer <API_KEY>
     * （智谱只认这个头，缺了就返回 401 鉴权失败）
     */
    private HttpHeaders jsonHeaders() {
        String apiKey = properties.getAi().getApiKey();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + (apiKey == null ? "" : apiKey.trim()));
        return headers;
    }

    /**
     * API Key 脱敏：只显示前 6 位 + 后 6 位，中间打码
     * 例：sk-ws-***(长度 94)***suHWXg
     */
    private String mask(String key) {
        if (!StringUtils.hasText(key)) {
            return "(空)";
        }
        String value = key.trim();
        if (value.length() <= 12) {
            return "***(长度" + value.length() + ")";
        }
        return value.substring(0, 6) + "***(长度" + value.length() + ")***" + value.substring(value.length() - 6);
    }

    /** 请求头里的 Authorization 脱敏后再打印 */
    private String maskHeader(HttpHeaders headers) {
        String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth)) {
            return "(缺失!)";
        }
        String prefix = BEARER_PREFIX;
        String token = auth.startsWith(prefix) ? auth.substring(prefix.length()) : auth;
        return (auth.startsWith(prefix) ? prefix : "(前缀不是Bearer )") + mask(token);
    }

    /** 启动时打印一次 AI 配置，便于确认 application.yml 是否被正确读到 */
    @jakarta.annotation.PostConstruct
    public void logAiConfig() {
        log.info("AI 配置：enabled={}, chat-url={}(写死), model={}, api-key={}",
                properties.getAi().getEnabled(),
                CHAT_URL,
                properties.getAi().getModel(),
                mask(properties.getAi().getApiKey()));
    }

    private boolean isAiEnabled() {
        return Boolean.TRUE.equals(properties.getAi().getEnabled())
                && StringUtils.hasText(properties.getAi().getApiKey());
    }

    /**
     * 余弦相似度
     */
    private double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        int len = Math.min(a.length, b.length);
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 简易关键词提取（去除常见停用词与标点）
     */
    public List<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return new ArrayList<>();
        }
        String cleaned = text.replaceAll("[\\p{Punct}\\s，。！？、；：“”‘’（）【】《》]", " ");
        List<String> stopWords = Arrays.asList(
                "的", "了", "是", "我", "想", "要", "有", "没有", "推荐", "几本", "一些", "好看", "请", "帮我",
                "什么", "怎么", "如何", "可以", "哪些", "这本", "那本", "图书", "书", "找", "看看", "一下");
        List<String> keywords = Arrays.stream(cleaned.split("\\s+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(w -> w.length() >= 1 && !stopWords.contains(w))
                .distinct()
                .collect(Collectors.toList());
        return keywords;
    }

    private String trim(String text, int length) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String value = text.replaceAll("\\s+", " ").trim();
        return value.length() <= length ? value : value.substring(0, length) + "…";
    }
}
