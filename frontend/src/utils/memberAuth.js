const MEMBER_TOKEN_KEY = 'sam-member-token'
const MEMBER_USER_KEY = 'sam-member-user'

export function getMemberToken() {
  return localStorage.getItem(MEMBER_TOKEN_KEY)
}

export function setMemberToken(token) {
  localStorage.setItem(MEMBER_TOKEN_KEY, token)
}

export function getMemberUser() {
  const user = localStorage.getItem(MEMBER_USER_KEY)
  return user ? JSON.parse(user) : null
}

export function setMemberUser(user) {
  localStorage.setItem(MEMBER_USER_KEY, JSON.stringify(user))
}

export function clearMemberAuth() {
  localStorage.removeItem(MEMBER_TOKEN_KEY)
  localStorage.removeItem(MEMBER_USER_KEY)
}
