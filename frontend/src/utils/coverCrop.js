/**
 * 封面裁剪参数的解析与渲染
 *
 * <p>参数由管理端的裁剪器生成并存成 JSON 字符串，坐标是<b>原图像素</b>：
 * <code>{"x":0,"y":0,"width":0,"height":0,"naturalWidth":0,"naturalHeight":0,"zoom":1}</code></p>
 *
 * <p>渲染思路：把原图按 100% / 裁剪区域宽度占比 放大后，用 translate 把裁剪区左上角对到容器左上角，
 * 因为 translate 的百分比是相对元素自身尺寸计算的，所以不用知道容器实际多大也不会变形。</p>
 */

/**
 * 解析裁剪参数，格式不对 / 为空时返回 null（表示不裁剪）
 *
 * @param {string|object} json 裁剪参数
 */
export function parseCrop(json) {
  if (!json) return null
  try {
    const crop = typeof json === 'string' ? JSON.parse(json) : json
    if (!crop || !crop.naturalWidth || !crop.naturalHeight || !crop.width || !crop.height) return null
    return crop
  } catch (e) {
    return null
  }
}

/**
 * 生成封面 img 的行内样式
 *
 * @param {string|object} json 裁剪参数，为空 / 解析失败时不返回任何样式（沿用原来的 object-fit: cover）
 * @returns {object} 行内样式对象
 */
export function cropImageStyle(json) {
  const crop = parseCrop(json)
  if (!crop) return {}

  return {
    position: 'absolute',
    top: '0',
    left: '0',
    // 宽度放大到「裁剪区刚好等于容器宽度」，高度保持原图比例
    width: `${(crop.naturalWidth / crop.width) * 100}%`,
    minWidth: '100%',
    minHeight: '100%',
    height: 'auto',
    objectFit: 'cover',
    // 把裁剪区左上角挪到容器左上角（百分比相对图片自身，和容器尺寸无关）
    transform: `translate(${(-(crop.x / crop.naturalWidth) * 100).toFixed(4)}%, ${(-(crop.y / crop.naturalHeight) * 100).toFixed(4)}%)`,
    transformOrigin: 'top left'
  }
}
