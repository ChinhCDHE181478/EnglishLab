package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.service.course.event.CourseThumbnailReplacedEvent;
import fu.sep490.g23.backend.service.payment.PaymentService;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import fu.sep490.g23.backend.service.schedule_job.LearningReminderService;
import fu.sep490.g23.backend.service.storage.ObjectStore;
import fu.sep490.g23.backend.service.user.event.AvatarUpdatedEvent;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import vn.payos.PayOS;

@TestPropertySource(properties={"englishlab.mail.enabled=true","englishlab.payos.enabled=false",
 "englishlab.payos.auto-confirm-webhook=false","englishlab.payos.reconciliation-initial-delay-ms=86400000",
 "spring.jpa.show-sql=false","logging.level.org.hibernate.SQL=OFF","logging.level.org.springframework.web=ERROR",
 "spring.mail.host=smtp.example.test","englishlab.mail.from=tests@example.test",
 "logging.level.org.springframework.jdbc=OFF","logging.level.org.springframework.security=ERROR",
 "app.seed.master.enabled=false","app.seed.test.enabled=false","app.seed.review.enabled=false","app.seed.sheet.enabled=false",
 "englishlab.payos.return-url=https://example.test/return","englishlab.payos.cancel-url=https://example.test/cancel"})
public abstract class WorkbookTestSupport {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate db;
 @Autowired EntityManager em;
 @Autowired PayosProperties payos;
 @MockitoBean JavaMailSender mail;
 @MockitoBean ObjectStore store;
 final Map<String,String> tokens=new HashMap<>();
 final Map<String,byte[]> stored=new HashMap<>();
 String caseId;
 @BeforeEach void start(TestInfo info) throws Exception {
  caseId=info.getTestMethod().orElseThrow().getName(); tokens.clear();stored.clear();
  assertTrue(db.queryForObject("select current_database()",String.class).startsWith("englishlab_it_"),"Only an isolated test DB is allowed");
  when(mail.createMimeMessage()).thenAnswer(i->new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
  when(store.objectKey(anyString(),anyString())).thenCallRealMethod();
  when(store.publicUrl(anyString())).thenAnswer(i->"https://storage.example.test/"+i.getArgument(0));
  when(store.isPublic()).thenReturn(true);
  when(store.put(anyString(),any(),anyLong(),anyString())).thenAnswer(i->{String k=i.getArgument(0); stored.put(k,((java.io.InputStream)i.getArgument(1)).readAllBytes());return new ObjectStore.StoredObject(k,"https://storage.example.test/"+k,i.getArgument(2),i.getArgument(3));});
  when(store.getBytes(anyString())).thenAnswer(i->Optional.ofNullable(stored.get(i.getArgument(0))));
  doAnswer(i->{stored.remove(i.getArgument(0));return null;}).when(store).delete(anyString());
  payos.setEnabled(false);
 }
 long n(String sql,Object... args){em.flush();return db.queryForObject(sql,Long.class,args);}
 String str(String sql,Object... args){em.flush();return db.queryForObject(sql,String.class,args);}
 void sql(String sql,Object...args){em.flush();db.update(sql,args);em.clear();}
 long uid(String email){return n("select id from users where email=?",email);}
 String jwt(String email)throws Exception{if(!tokens.containsKey(email))tokens.put(email,login(mvc,email,PASSWORD));return tokens.get(email);}
 String body(Object... kv)throws Exception{Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)m.put((String)kv[i],kv[i+1]);return mapper().writeValueAsString(m);}
 MvcResult call(String method,String path,String email,String body)throws Exception{
  var req=request(HttpMethod.valueOf(method),path);if(email!=null)req.header("Authorization",bearer(jwt(email)));if(body!=null)req.contentType(MediaType.APPLICATION_JSON).content(body);
  var r=mvc.perform(req).andReturn();System.out.println("WORKBOOK_EVIDENCE "+caseId+" "+method+" "+path+" HTTP="+r.getResponse().getStatus());return r;
 }
 JsonNode ok(String method,String path,String email,String body)throws Exception{var r=call(method,path,email,body);assertTrue(r.getResponse().getStatus()>=200&&r.getResponse().getStatus()<300,()->path+" HTTP="+r.getResponse().getStatus()+" "+new String(r.getResponse().getContentAsByteArray(),java.nio.charset.StandardCharsets.UTF_8));return r.getResponse().getContentAsByteArray().length==0?mapper().nullNode():json(r);}
 MvcResult rejected(String method,String path,String email,String body)throws Exception{var r=call(method,path,email,body);assertTrue(r.getResponse().getStatus()>=400&&r.getResponse().getStatus()<500,()->"Expected rejection: "+path+" HTTP="+r.getResponse().getStatus());return r;}
 void status(MvcResult r,int expected){assertEquals(expected,r.getResponse().getStatus(),caseId+" HTTP status");}
 JsonNode row(JsonNode rows,String key,long id){for(var x:items(rows))if(x.path(key).asLong()==id)return x;return mapper().missingNode();}
 String freshLearner()throws Exception{
  String email="pending."+UUID.randomUUID()+"@englishlab-it.test";
  // Clone only a verified learner fixture; never send registration e-mail to a real recipient.
  long id=n("insert into users(email,password,full_name,email_verified,password_set,profile_completed,notification_class_reminder_enabled,notification_email_enabled,notification_in_app_enabled,notification_study_alert_enabled,teacher_public_profile,created_at,updated_at) select ?,password,'Round1 Fixture',true,true,false,true,true,true,true,false,current_timestamp,current_timestamp from users where email=? returning id",email,LEARNER);
  sql("insert into user_roles(user_id,role_code) values(?,'LEARNER')",id);return email;
 }
 long course(int price,int lessons)throws Exception{
  List<Object> ls=new ArrayList<>();for(int i=0;i<lessons;i++)ls.add(Map.of("title","Lesson "+(i+1),"sequenceNumber",i+1,"contentType","TEXT","contentText","Integration fixture content"));
  long id=ok("POST","/api/content-manager/online-courses",CM,body("title","Round1 "+UUID.randomUUID(),"category","IELTS","level","BEGINNER","targetBand",6.5,"targetScore","6.5","targetOutcome","Read and write English at IELTS band 6.5","status","DRAFT","price",price,"modules",lessons==0?List.of():List.of(Map.of("title","Module 1","sequenceNumber",1,"lessons",ls)))).path("id").asLong();
  // These cases start with a published course, including the explicit no-assessment fixture
  // in IT_LEARNING_05. Seed that precondition; publication validation is tested separately.
  if(lessons>0){sql("update online_courses set status='PUBLISHED' where id=?",id);sql("update online_course_versions set status='PUBLISHED',published_at=current_timestamp,total_required_lessons=?,total_required_assessments=0 where online_course_id=?",lessons,id);}return id;
 }
 void enroll(String learner,long cid)throws Exception{ok("POST","/api/student/online-courses/"+cid+"/register",learner,null);}
 String content(long c){return "/api/student/online-courses/"+c+"/content";}
 long enrollment(String learner,long c){return n("select id from online_course_enrollments where student_id=? and online_course_id=?",uid(learner),c);}
 long lesson(long c,int seq){return n("select l.id from online_lessons l join online_course_modules m on m.id=l.module_id join online_course_versions v on v.id=m.online_course_version_id where v.online_course_id=? and v.status='PUBLISHED' order by l.id offset ? limit 1",c,seq);}
 String progress(long c,long l,boolean completed){return "/api/student/online-courses/"+c+"/lessons/"+l+"/progress?completed="+completed;}
 long shared(){return n("select c.id from class_sections c join class_enrollments e on e.class_section_id=c.id where c.primary_teacher_id=? and e.student_id=? and e.registration_status='ASSIGNED' order by c.id limit 1",uid(TEACHER),uid(LEARNER));}
 long session(long c){return n("select id from class_schedules where class_section_id=? order by id limit 1",c);}
 long otherClass(){return n("select id from class_sections where primary_teacher_id<>? order by id limit 1",uid(TEACHER));}
 long note(String email,String type,String title){return n("insert into app_notifications(user_id,type,title,body,read,created_at) values(?,?,?,'Round1 evidence',false,current_timestamp) returning id",uid(email),type,title);}
 String snapshot(String table){em.flush();return str("select coalesce(jsonb_agg(to_jsonb(t) order by id)::text,'[]') from "+table+" t");}
 long cloneRow(String table,long source,Object... overrides)throws Exception{
  String columns=String.join(",",db.queryForList("select column_name from information_schema.columns where table_schema='public' and table_name=? and column_name<>'id' order by ordinal_position",String.class,table));
  return n("insert into "+table+" ("+columns+") select "+columns+" from jsonb_populate_record(null::"+table+",(select to_jsonb(t) from "+table+" t where id=?) || ?::jsonb) returning id",source,body(overrides));
 }
 long classroom()throws Exception{
  long source=shared();return cloneRow("class_sections",source,"name","Round1 classroom","code","R1-"+UUID.randomUUID().toString().substring(0,8),"capacity",100,"status","UPCOMING","start_date",java.time.LocalDate.now().plusDays(10).toString(),"planned_end_date",java.time.LocalDate.now().plusDays(60).toString(),"delivery_mode","VIRTUAL","room_id",null,"google_meet_url","https://meet.google.com/abc-defg-hij","google_meet_status","READY");
 }
 long classEnroll(long c,String u)throws Exception{
  return cloneRow("class_enrollments",n("select id from class_enrollments where class_section_id=? and student_id=?",shared(),uid(LEARNER)),"class_section_id",c,"student_id",uid(u),"registration_status","ASSIGNED","gradebook_status","GRADED","homework_score",null,"attendance_percent",null,"teacher_comment",null,"transferred_from_enrollment_id",null);
 }
 long classSession(long c)throws Exception{
  return cloneRow("class_schedules",session(shared()),"class_section_id",c,"teacher_id",uid(TEACHER),"session_date",java.time.LocalDate.now().plusDays(10).toString(),"start_time","10:00:00","end_time","11:00:00","status","SCHEDULED","delivery_mode_override",null,"room_id",null);
 }
 String otherTeacher()throws Exception{String u=freshLearner();sql("insert into user_roles(user_id,role_code) values(?,'TEACHER')",uid(u));return u;}

 @Autowired LearningReminderService reminders;
@Autowired PasswordEncoder encoder;
 @Autowired PaymentService payments;
 
 
 
 
 
 
 MockedConstruction<PayOS> provider(boolean paid){
  payos.setEnabled(true);payos.setClientId("round1-stub");payos.setApiKey("round1-stub");payos.setChecksumKey("round1-checksum");
  return mockConstruction(PayOS.class,(client,ctx)->{
   var service=mock(vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class,RETURNS_DEEP_STUBS);
   var link=mock(vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse.class);
   when(link.getPaymentLinkId()).thenReturn("round1-link");when(link.getCheckoutUrl()).thenReturn("https://pay.example.test/round1");when(link.getQrCode()).thenReturn("round1-qr");
   when(service.create(any())).thenReturn(link);when(client.paymentRequests()).thenReturn(service);
   when(service.get(anyLong()).getStatus()).thenReturn(paid?vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PAID:vn.payos.model.v2.paymentRequests.PaymentLinkStatus.PENDING);
   when(service.get(anyLong()).getId()).thenReturn("round1-link");
   var crypto=mock(vn.payos.crypto.CryptoProvider.class);when(crypto.createSignatureFromObj(any(),anyString())).thenReturn("valid-round1-signature");when(client.getCrypto()).thenReturn(crypto);
  });
 }
 long order(String u,long c,String coupon)throws Exception{ok("POST","/api/student/commerce/cart/"+c,u,null);return ok("POST","/api/student/payments/payos/link",u,body("courseIds",List.of(c),"couponCode",coupon)).path("orderCode").asLong();}
 void webhook(long code,boolean success)throws Exception{ok("POST","/api/payos/webhook",null,body("code","00","success",success,"signature","valid-round1-signature","data",Map.of("orderCode",code,"code",success?"00":"01","reference","round1-ref")));}
 String orderStatus(long code){return str("select status from payment_orders where order_code=?",code);}
 String coupon()throws Exception{String code="R1"+UUID.randomUUID().toString().replace("-","").substring(0,10).toUpperCase();ok("POST","/api/content-manager/discount-codes",CM,body("code",code,"name","Ten percent","type","PERCENTAGE","value",10,"usageLimit",5,"active",true));return code;}
 void couponCounts(String code,int used,int reserved,int remaining)throws Exception{var all=ok("GET","/api/content-manager/discount-codes",CM,null);com.fasterxml.jackson.databind.JsonNode item=mapper().missingNode();for(var x:items(all))if(code.equals(x.path("code").asText()))item=x;assertEquals(used,item.path("usedCount").asInt(-1));assertEquals(reserved,item.path("reservedCount").asInt(-1));assertEquals(remaining,item.path("remainingUses").asInt(-1));}

long ticket()throws Exception{return ok("POST","/api/student/support-tickets",LEARNER,body("subject","Round1 integration support","category","TECHNICAL","message","Please review this integration fixture")).path("id").asLong();}
 String learnerTicket(long k){return "/api/student/support-tickets/"+k;}
 String staffTicket(long k){return "/api/staff/support-tickets/"+k;}
 void claim(long k)throws Exception{ok("POST",staffTicket(k)+"/claim",STAFF,null);}
 void reply(long k)throws Exception{ok("POST",staffTicket(k)+"/replies",STAFF,body("message","Round1 staff answer for the learner"));}
 String ticketStatus(long k){return str("select status from support_tickets where id=?",k);}
 long notifCount(String u,String type){return n("select count(*) from app_notifications where user_id=? and type=?",uid(u),type);}
 
 
 
 
 
 
 
 
 
 long broadcast(boolean inApp,boolean email,String path)throws Exception{return ok("POST","/api/admin/broadcasts",ADMIN,body("title","Round1 "+UUID.randomUUID(),"message","Round1 broadcast fixture","sendInApp",inApp,"sendEmail",email,"targetRole","LEARNER","actionPath",path)).path("id").asLong();}

long program(){return n("select instructor_led_course_id from class_sections where id=?",shared());}
 String requestBody(String u,long p)throws Exception{return body("courseOfferingId",p,"contactName","Round1 learner","contactEmail",u,"contactPhone","0901234567","consultationTrack","IELTS");}
 long registration(String u,long p)throws Exception{return ok("POST","/api/student/course-enrollment-requests",u,requestBody(u,p)).path("id").asLong();}
 String reqPath(long r){return "/api/staff/enrollment-requests/"+r;}
 void schedule(long r)throws Exception{ok("PATCH",reqPath(r)+"/schedule-test",STAFF,body("appointmentAt",LocalDateTime.now().plusDays(1).withNano(0).toString(),"location","Round1 test room"));}
 void eligible(long r)throws Exception{schedule(r);ok("PATCH",reqPath(r)+"/complete-test",STAFF,body("eligible",true,"placementLevel","INTERMEDIATE"));}
 String regStatus(long r){return str("select status from course_registration_requests where id=?",r);}
 long historyCount(long r){return n("select count(*) from system_audit_logs where target_type='COURSE_REGISTRATION_REQUEST' and target_id=?",String.valueOf(r));}
 void assigned(String u,long r,long c)throws Exception{assertEquals("CLASS_ASSIGNED",regStatus(r));assertEquals(c,n("select assigned_class_section_id from course_registration_requests where id=?",r));assertEquals(1,n("select count(*) from class_enrollments where class_section_id=? and student_id=? and registration_status='ASSIGNED'",c,uid(u)));assertFalse(row(ok("GET","/api/student/classrooms/my-classrooms",u,null),"id",c).isMissingNode());}
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 LocalDate monday(){return LocalDate.now().plusMonths(3).with(TemporalAdjusters.next(DayOfWeek.MONDAY));}
 String proposalBody(long teacher)throws Exception{return body("title","Round1 proposal","courseOfferingId",program(),"primaryTeacherId",teacher,"capacity",20,"plannedStartDate",monday().toString(),"endDate",monday().plusWeeks(4).toString(),"weekdays",List.of("MONDAY","WEDNESDAY"),"sessionStartTime","18:00","sessionEndTime","20:00","deliveryType","VIRTUAL");}
 long proposal(long teacher)throws Exception{return ok("POST","/api/staff/classroom-proposals",STAFF,proposalBody(teacher)).path("id").asLong();}
 void send(long q)throws Exception{ok("PATCH","/api/staff/classroom-proposals/"+q+"/submit",STAFF,null);}
 String proposalStatus(long q){return str("select approval_status from classroom_proposals where id=?",q);}

 long homework(long c,String state)throws Exception{return ok("POST","/api/teacher/classrooms/"+c+"/homework",TEACHER,hwBody(state,java.time.LocalDateTime.now().plusDays(3).withNano(0).toString())).path("id").asLong();}
 String hwBody(String state,String deadline)throws Exception{return body("title","Round1 homework","instruction","Write a short paragraph","status",state,"deadline",deadline,"maxScore",10,"allowResubmission",false,"activityType","TEXT_RESPONSE","skill","WRITING","gradingMode","TEACHER","aiReviewEnabled",false);}
 void submit(long h,String answer)throws Exception{ok("POST","/api/student/classrooms/homework/"+h+"/submit",LEARNER,body("textAnswer",answer));}
 long attended(long c,long s,String state)throws Exception{ok("POST","/api/teacher/classrooms/attendance",TEACHER,body("sessionId",s,"records",List.of(Map.of("studentId",uid(LEARNER),"status",state))));return n("select id from classroom_attendance_records where session_id=? and student_id=?",s,uid(LEARNER));}
 long dispute(long a)throws Exception{return ok("POST","/api/student/attendance/"+a+"/disputes",LEARNER,body("reason","I attended via the online link")).path("id").asLong();}
 long material(long c)throws Exception{return ok("POST","/api/teacher/classrooms/"+c+"/materials",TEACHER,body("title","Round1 material","fileUrl","https://example.test/material.pdf","fileType","PDF","visibility","CLASSROOM","sourceType","TEACHER_UPLOAD")).path("id").asLong();}
 long change(long c,long s)throws Exception{return ok("POST","/api/teacher/classrooms/requests",TEACHER,body("requestType","RESCHEDULE_SESSION","classSectionId",c,"targetSessionId",s,"newValuesJson",body("sessionDate",java.time.LocalDate.now().plusDays(12).toString(),"startTime","13:00","endTime","14:00"),"reason","Round1 schedule request")).path("id").asLong();}
 
 
 
 
 
 
 
 
 
 
 
 long gradedFixture(double score)throws Exception{long c=classroom();classEnroll(c,LEARNER);long h=homework(c,"OPEN");submit(h,"Round1 submitted answer");ok("POST","/api/teacher/classrooms/homework/"+h+"/students/"+uid(LEARNER)+"/grade",TEACHER,body("score",score,"teacherFeedback",score==9?"Well done":"Good"));return h;}

@Autowired ApplicationEvents events;

 long unit(long c){return n("select u.id from course_units u join class_sections c on c.instructor_led_course_id=u.instructor_led_course_id where c.id=? order by u.id limit 1",c);}
 String exerciseBody(String state)throws Exception{return body("title","Round1 practice","skill","VOCABULARY","exerciseType","PRACTICE","prompt","Choose answers for q1, q2, q3 and q4","answerKey",body("q1","A","q2","B","q3","C","q4","D"),"status",state);}
 long exercise(String state)throws Exception{return ok("POST","/api/content-manager/exercise-bank",CM,exerciseBody(state)).path("id").asLong();}
 void attach(long c,long e)throws Exception{ok("POST","/api/content-manager/curriculum-units/"+unit(c)+"/exercises",CM,body("resourceId",e,"displayOrder",1));}
 String practice(long c){return "/api/student/classrooms/"+c+"/practice";}
 String answers(boolean all)throws Exception{return body("answersJson",body("q1","A","q2","B","q3",all?"C":"X","q4",all?"D":"X"),"durationSeconds",60);}
 
 
 
 
 
 
 
 
 
 com.fasterxml.jackson.databind.JsonNode upload(String path,String user)throws Exception{var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(8,8,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);byte[] png=bytes.toByteArray();var r=mvc.perform(multipart(path).file(new MockMultipartFile("file","test.png","image/png",png)).header("Authorization",bearer(jwt(user)))).andReturn();System.out.println("ROUND1_EVIDENCE "+caseId+" UPLOAD "+path+" HTTP="+r.getResponse().getStatus());status(r,200);return json(r);}
 void commit(){TestTransaction.flagForCommit();TestTransaction.end();TestTransaction.start();}

long paid(long c,String u) throws Exception {long e=inactive(c,u);sql("update class_enrollments set registration_status='FULLY_PAID',tuition_amount_paid=tuition_amount_due where id=?",e);return e;}
 void assignPaid(long e) throws Exception {ok("POST","/api/staff/classrooms/enrollments/"+e+"/assign",STAFF,body("assignmentNote","Retest assignment"));}
 long inactive(long c,String u) throws Exception {long e=classEnroll(c,u);sql("update class_enrollments set registration_status='CANCELLED',gradebook_status='PENDING' where id=?",e);return e;}

}
