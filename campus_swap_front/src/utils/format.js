import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

/** 格式化日期 */
export const formatDate = (date, fmt = 'YYYY-MM-DD') => dayjs(date).format(fmt)

/** 相对时间（如：3小时前） */
export const fromNow = (date) => dayjs(date).fromNow()

/** 格式化价格 */
export const formatPrice = (price) =>
  Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
