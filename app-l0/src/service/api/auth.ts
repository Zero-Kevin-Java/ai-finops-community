import { request } from '../request';

/** Get tenant list */
export function fetchTenantList() {
  return request<Api.Auth.LoginTenant>({
    url: '/auth/tenant/list',
    method: 'get'
  });
}

/** Get image code */
export function fetchCaptchaCode() {
  return request<Api.Auth.CaptchaCode>({
    url: '/auth/code',
    method: 'get'
  });
}

/** Send SMS code */
export function fetchSmsCode(phonenumber: string) {
  return request<void>({
    url: '/resource/sms/code',
    method: 'get',
    params: { phonenumber }
  });
}

/**
 * Login
 *
 * @param username User name
 * @param password Password
 */
export function fetchLogin(
  data: Api.Auth.PwdLoginForm | Api.Auth.SmsLoginForm | Api.Auth.EmailLoginForm | Api.Auth.SocialLoginForm
) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/login',
    method: 'post',
    headers: {
      isToken: false,
      isEncrypt: true,
      repeatSubmit: false
    },
    data
  });
}

/** social login callback */
export function fetchSocialLoginCallback(data: Api.Auth.SocialLoginForm) {
  return request({
    url: '/auth/social/callback',
    method: 'post',
    data
  });
}

/** Register */
export function fetchRegister(data: Api.Auth.RegisterForm) {
  return request<Api.Auth.LoginToken>({
    url: '/auth/register',
    method: 'post',
    headers: {
      isToken: false,
      isEncrypt: true,
      repeatSubmit: false
    },
    data
  });
}

/** Get user info */
export function fetchGetUserInfo() {
  return request<Api.Auth.UserInfo>({ url: '/system/user/getInfo' });
}

/** Logout */
export function fetchLogout() {
  if (import.meta.env.VITE_APP_SSE === 'Y') {
    request({
      url: '/resource/sse/close',
      method: 'get'
    });
  }
  return request({
    url: '/auth/logout',
    method: 'post'
  });
}

/** Leave current tenant */
export function fetchLeaveTenant() {
  return request<Api.Auth.TenantLeaveResult>({
    url: '/auth/tenant/leave',
    method: 'delete'
  });
}

/** Leave specified tenant */
export function fetchLeaveTenantById(tenantId: CommonType.IdType) {
  return request<Api.Auth.TenantLeaveResult>({
    url: `/auth/tenant/leave/${tenantId}`,
    method: 'delete'
  });
}
