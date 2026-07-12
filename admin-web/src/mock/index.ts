import type { AxiosAdapter, AxiosResponse } from 'axios'
import ExcelJS from 'exceljs'
import type { BusinessType, Order, SourceChannel } from '../types/api'

const names=['张明远','李欣怡','王浩然','陈雨桐','刘子轩','周思涵','赵文博','孙可欣','吴嘉诚','郑晓彤']
const agents=['张三','李四','王五','赵六','陈晨']
const types:BusinessType[]=['CAMPUS_CARD','CAMPUS_NETWORK','DRIVING_SCHOOL','RENEWAL']
const sources:SourceChannel[]=['ONLINE','AGENT','STORE']
const mockAgents=agents.map((name,index)=>({id:index+1,agentNo:`AG202607${String(index+1).padStart(4,'0')}`,name,phone:`1380000000${index+3}`,level:['NORMAL','ADVANCED','CAMPUS_LEADER'][index%3],status:1,todayOrders:12-index,monthOrders:68-index*7,createdAt:`2026-07-0${index+1} 10:00:00`}))
const mockIssues=[{id:1,issueNo:'ISSUE20260711000001',submitterName:'张三',submitterType:'AGENT',contactPhone:'13800000003',issueType:'CAMPUS_NETWORK',description:'校园网办理后暂时无法使用',status:'PENDING',submittedAt:'2026-07-11 09:30:00'},{id:2,issueNo:'ISSUE20260710000002',submitterName:'普通用户',submitterType:'USER',contactPhone:'13800000004',issueType:'ACCOUNT',description:'忘记密码，需要协助处理',status:'PROCESSING',submittedAt:'2026-07-10 16:20:00'}]
let orders:Order[]=Array.from({length:68},(_,index)=>{
  const businessType=types[index%4]!,day=String((index%28)+1).padStart(2,'0')
  return {id:index+1,orderNo:`${{CAMPUS_CARD:'CARD',CAMPUS_NETWORK:'NET',DRIVING_SCHOOL:'DRIVE',RENEWAL:'RENEW'}[businessType]}202607${day}${String(index+1).padStart(6,'0')}`,businessType,name:names[index%names.length]!,phone:`138${String(12000000+index*791).slice(-8)}`,businessNumber:businessType==='DRIVING_SCHOOL'?'':`186${String(50000000+index*113).slice(-8)}`,sourceChannel:sources[index%3]!,agentId:index%3===2?undefined:(index%agents.length)+1,agentName:index%3===2?undefined:agents[index%agents.length],auditStatus:index%5===0?'PENDING':'CONFIRMED',exportStatus:businessType==='CAMPUS_NETWORK'?(index%3===0?'EXPORTED':'NOT_EXPORTED'):undefined,createdAt:`2026-07-${day} ${String(8+index%11).padStart(2,'0')}:${String(index*7%60).padStart(2,'0')}:00`,remark:index%6===0?'学生已电话确认':undefined,deleted:index===12||index===39}
})

const ok=<T>(config:Parameters<AxiosAdapter>[0],data:T,status=200):AxiosResponse=>({data:{code:200,message:'success',data},status,statusText:'OK',headers:{},config})
const wait=(ms=180)=>new Promise(resolve=>setTimeout(resolve,ms))
const urlPath=(url='')=>url.replace(/^https?:\/\/[^/]+/,'').replace(/^\/api/,'')

export const mockAdapter:AxiosAdapter=async config=>{
  await wait()
  const path=urlPath(config.url),method=(config.method||'get').toLowerCase()
  if(path==='/auth/login'&&method==='post') return ok(config,{token:'mock-owner-token',user:{id:1,username:'admin',realName:'系统老板',role:'OWNER'}})
  if(path==='/auth/me') return ok(config,{id:1,username:'admin',realName:'系统老板',role:'OWNER'})
  if(path==='/auth/logout') return ok(config,null)
  if(path==='/dashboard/cards') return ok(config,types.map((businessType,i)=>({businessType,today:[12,8,5,9][i],month:[116,83,42,97][i]})))
  if(path==='/dashboard/trend') {
    const params=config.params||{},range=params.range
    if(range==='custom'&&params.start&&params.end){
      const start=new Date(`${params.start}T00:00:00`),end=new Date(`${params.end}T00:00:00`),days=Math.max(1,Math.min(366,Math.floor((end.getTime()-start.getTime())/86400000)+1))
      return ok(config,Array.from({length:days},(_,i)=>{const day=new Date(start);day.setDate(day.getDate()+i);return{date:`${day.getMonth()+1}-${String(day.getDate()).padStart(2,'0')}`,count:5+(i*7+i%3*4)%18}}))
    }
    const count=range==='30d'?30:range==='month'?11:7
    return ok(config,Array.from({length:count},(_,i)=>({date:`07-${String(i+1).padStart(2,'0')}`,count:5+(i*7+i%3*4)%18})))
  }
  if(path==='/dashboard/agent-ranking') return ok(config,agents.map((agentName,i)=>({agentId:i+1,agentName,count:[28,23,19,14,9][i]})))
  if(path==='/dashboard/todos') return ok(config,{pendingOrders:14,unexportedNetworks:17,pendingIssues:6,processingIssues:3,monthlyAgentOrders:246})
  if(path==='/agents'&&method==='get'){const p=config.params||{},keyword=String(p.keyword||'').toLowerCase();const rows=mockAgents.filter(a=>(p.status===undefined||p.status===''||a.status===Number(p.status))&&(!keyword||[a.agentNo,a.name,a.phone].some(value=>value.toLowerCase().includes(keyword))));return ok(config,{records:rows,total:rows.length,page:Number(p.page||1),pageSize:Number(p.pageSize||100)})}
  if(path?.match(/^\/agents\/\d+$/)&&method==='get')return ok(config,mockAgents.find(a=>a.id===Number(path.split('/').pop())))
  if(path==='/issues'&&method==='get'){const p=config.params||{},keyword=String(p.keyword||'').toLowerCase();const rows=mockIssues.filter(i=>(!p.status||i.status===p.status)&&(!p.issueType||i.issueType===p.issueType)&&(!keyword||[i.issueNo,i.submitterName,i.contactPhone,i.description].some(value=>value.toLowerCase().includes(keyword))));return ok(config,{records:rows,total:rows.length,page:Number(p.page||1),pageSize:Number(p.pageSize||20)})}
  if(path?.match(/^\/issues\/\d+$/)&&method==='get')return ok(config,{...mockIssues.find(i=>i.id===Number(path.split('/').pop())),images:[]})
  if(path==='/configs'&&method==='get')return ok(config,[{configKey:'duplicateWindowDays',configValue:'30',description:'重复订单限制天数'},{configKey:'drivingC1NormalPrice',configValue:'2800',description:'C1普通班默认价格'},{configKey:'drivingC1FullPrice',configValue:'3600',description:'C1全包班默认价格'},{configKey:'drivingC2NormalPrice',configValue:'3000',description:'C2普通班默认价格'},{configKey:'drivingC2FullPrice',configValue:'3900',description:'C2全包班默认价格'}])
  if(path==='/system/users'&&method==='get')return ok(config,{records:[{id:1,username:'admin',realName:'系统老板',phone:'13800000001',role:'OWNER',status:1,createdAt:'2026-07-01 09:00:00'},{id:2,username:'staff',realName:'门店管理员',phone:'13800000002',role:'ADMIN',status:1,createdAt:'2026-07-01 09:10:00'}],total:2,page:1,pageSize:20})
  if(path==='/operation-logs'&&method==='get')return ok(config,{records:[{id:1,operatorName:'系统老板',module:'ORDER',operationType:'CONFIRM',targetId:2,operationDescription:'确认校园网订单',ipAddress:'127.0.0.1',createdAt:'2026-07-11 10:20:00'}],total:1,page:1,pageSize:20})
  if(path==='/orders'&&method==='get'){
    const p=config.params||{},keyword=String(p.keyword||'').trim().toLowerCase();let rows=orders.filter(o=>(p.includeDeleted?true:!o.deleted)&&(!p.businessType||o.businessType===p.businessType)&&(!p.sourceChannel||o.sourceChannel===p.sourceChannel)&&(!p.agentId||o.agentId===Number(p.agentId))&&(!p.auditStatus||o.auditStatus===p.auditStatus)&&(!p.exportStatus||o.exportStatus===p.exportStatus)&&(!p.startTime||o.createdAt.slice(0,10)>=p.startTime)&&(!p.endTime||o.createdAt.slice(0,10)<=p.endTime)&&(!keyword||[o.orderNo,o.name,o.phone,o.businessNumber,o.agentName,o.remark].some(value=>String(value||'').toLowerCase().includes(keyword)))).sort((a,b)=>b.createdAt.localeCompare(a.createdAt)||b.id-a.id)
    const page=Number(p.page||1),pageSize=Number(p.pageSize||20),total=rows.length;rows=rows.slice((page-1)*pageSize,page*pageSize);return ok(config,{records:rows,total,page,pageSize})
  }
  if(path?.match(/^\/orders\/\d+$/)&&method==='get'){const row=orders.find(o=>o.id===Number(path.split('/').pop()));return ok(config,row?{...row,name:row.name,phone:row.phone}:null)}
  const match=path?.match(/^\/orders\/(\d+)(?:\/(confirm|restore))?$/)
  if(match){const row=orders.find(o=>o.id===Number(match[1]));if(row){if(match[2]==='confirm')row.auditStatus='CONFIRMED';else if(match[2]==='restore')row.deleted=false;else if(method==='delete')row.deleted=true}return ok(config,null)}
  if(path==='/orders/campus-network/export'){
    const p=config.params||{},orderId=Number(p.orderId||0)
    const rows=orders.filter(o=>o.businessType==='CAMPUS_NETWORK'&&!o.deleted&&o.auditStatus==='CONFIRMED'&&(!orderId||o.id===orderId)&&(!p.sourceChannel||o.sourceChannel===p.sourceChannel)&&(!p.agentId||o.agentId===Number(p.agentId))&&(!p.exportStatus||o.exportStatus===p.exportStatus)&&(!p.startTime||o.createdAt.slice(0,10)>=p.startTime)&&(!p.endTime||o.createdAt.slice(0,10)<=p.endTime))
    const workbook=new ExcelJS.Workbook(),sheet=workbook.addWorksheet('校园网订单')
    sheet.columns=[{header:'姓名',key:'name',width:14},{header:'联系电话',key:'phone',width:18},{header:'新办号码',key:'businessNumber',width:18},{header:'学号',key:'studentNo',width:18},{header:'身份证后六位',key:'idSuffix',width:18}]
    rows.forEach((row,index)=>sheet.addRow({name:row.name,phone:row.phone,businessNumber:row.businessNumber,studentNo:`202607${String(row.id).padStart(4,'0')}`,idSuffix:String(320100+index).slice(-6)}))
    sheet.getRow(1).font={bold:true};sheet.getRow(1).alignment={horizontal:'center'}
    const buffer=await workbook.xlsx.writeBuffer();rows.forEach(row=>row.exportStatus='EXPORTED')
    return {data:new Blob([new Uint8Array(buffer)],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}),status:200,statusText:'OK',headers:{},config}
  }
  return ok(config,null)
}
