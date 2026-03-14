/**
 * 全局弹窗通知工具
 * 用 ElMessageBox.alert 以居中弹窗形式展示所有提示信息。
 * - customClass 'cs-notify' 对应 src/assets/styles/notify.scss 中的样式
 * - 单例锁：同一时间只显示一个弹窗，防止叠加
 */
import { ElMessageBox } from 'element-plus'
// 确保 ElMessageBox 的 CSS 在程序化调用时也被加载
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/overlay/style/css'

const TITLE = {
  success: '操作成功',
  error:   '出错了',
  warning: '注意',
  info:    '提示',
}

let _open = false

/**
 * 显示居中弹窗提示
 * @param {'success'|'error'|'warning'|'info'} type
 * @param {string} message
 * @returns {Promise<void>}  可 await，用户点击「确定」后 resolve
 */
export function showMsg(type, message) {
  if (_open) return Promise.resolve()
  _open = true
  return ElMessageBox.alert(message, TITLE[type] || '提示', {
    type,
    customClass: 'cs-notify',
    confirmButtonText: '确定',
    showClose: false,
    closeOnClickModal: false,
  }).finally(() => {
    _open = false
  })
}

export const showSuccess = (msg) => showMsg('success', msg)
export const showError   = (msg) => showMsg('error',   msg)
export const showWarning = (msg) => showMsg('warning',  msg)
export const showInfo    = (msg) => showMsg('info',     msg)
