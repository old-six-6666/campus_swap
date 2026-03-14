import request from '@/api/index'

export const userApi = {
  /** 发送验证码 */
  sendCode: (data) => request.post('/user/send-code', data),

  /** 登录 */
  login: (data) => request.post('/user/login', data),

  /** 注册 */
  register: (data) => request.post('/user/register', data),

  /** 忘记密码 */
  forgotPassword: (data) => request.post('/user/forgot-password', data),

  /** 获取当前用户信息 */
  getProfile: () => request.get('/user/profile'),

  /** 更新用户信息 */
  updateProfile: (data) => request.put('/user/profile', data),

  /** 通过旧密码修改密码 */
  changePasswordByOld: (data) => request.put('/user/password', data),

  /** 通过邮箱验证码修改密码（需先发送验证码） */
  changePasswordByEmail: (data) => request.put('/user/password/by-email', data),

  /** 退出登录 */
  logout: () => request.post('/user/logout'),

  /** 提交学生认证申请 */
  applyVerify: (data) => request.post('/user/verify', data),

  /** 查询当前用户的认证申请状态 */
  getMyVerification: () => request.get('/user/verify'),
}
