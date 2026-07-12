import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import request from '../utils/request'
import type { ApiResponse, UserInfo } from '../types/api'

export const useAuthStore=defineStore('auth',()=>{
  const token=ref(localStorage.getItem('campus_token')||'')
  const user=ref<UserInfo|null>(JSON.parse(localStorage.getItem('campus_user')||'null'))
  const isOwner=computed(()=>user.value?.role==='OWNER')
  async function login(username:string,password:string){const res=await request.post<never,ApiResponse<{token:string;user:UserInfo}>>('/auth/login',{username,password});token.value=res.data.token;user.value=res.data.user;localStorage.setItem('campus_token',token.value);localStorage.setItem('campus_user',JSON.stringify(user.value))}
  async function fetchMe(){const res=await request.get<never,ApiResponse<UserInfo>>('/auth/me');user.value=res.data;localStorage.setItem('campus_user',JSON.stringify(user.value))}
  async function logout(){try{await request.post('/auth/logout')}finally{token.value='';user.value=null;localStorage.removeItem('campus_token');localStorage.removeItem('campus_user')}}
  return{token,user,isOwner,login,fetchMe,logout}
})
