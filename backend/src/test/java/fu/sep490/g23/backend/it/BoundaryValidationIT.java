package fu.sep490.g23.backend.it;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import vn.payos.PayOS;
import org.mockito.MockedConstruction;
import java.util.List;

import static fu.sep490.g23.backend.it.ItSupport.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Boundary and validation procedures for integration testing.
 * Run on an isolated fixture database. Every test rolls back through EnglishLabIT.
 * Assertions deliberately preserve the workbook's exact HTTP expectations.
 * Only the external PayOS SDK is stubbed; controllers, services and PostgreSQL are real.
 */
@EnglishLabIT
@TestPropertySource(properties = {
    "logging.level.org.springframework.web=ERROR", "logging.level.org.hibernate.SQL=OFF",
    "logging.level.org.springframework.security=ERROR", "spring.jpa.show-sql=false",
    "spring.task.scheduling.enabled=false", "app.seed.master.enabled=false",
    "englishlab.payos.return-url=https://example.test/it-return",
    "englishlab.payos.cancel-url=https://example.test/it-cancel"
})
public class BoundaryValidationIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @Autowired EntityManager em;
    @Autowired PayosProperties payos;
    static final long MISSING = 999999991L;

    long scalar(String sql, Object... args) { return db.queryForObject(sql, Long.class, args); }
    long user(String email) { return scalar("select id from users where email=?", email); }
    String token(String email) throws Exception { return login(mvc, email, PASSWORD); }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String email) throws Exception {
        return req.header("Authorization", bearer(token(email)));
    }
    String snapshot(String table) {
        em.flush();
        return db.queryForObject("select coalesce(jsonb_agg(to_jsonb(t) order by id)::text,'[]') from " + table + " t", String.class);
    }
    MvcResult perform(String id, MockHttpServletRequestBuilder req) throws Exception {
        MvcResult r = mvc.perform(req).andReturn();
        System.out.println("WORKBOOK_EVIDENCE " + id + " HTTP=" + r.getResponse().getStatus());
        return r;
    }
    void unchanged(String id, MockHttpServletRequestBuilder req, int expected, String... tables) throws Exception {
        List<String> before = java.util.Arrays.stream(tables).map(this::snapshot).toList();
        MvcResult r = perform(id, req);
        for (int i=0;i<tables.length;i++) assertEquals(before.get(i), snapshot(tables[i]), id+" DB changed: "+tables[i]);
        System.out.println("WORKBOOK_EVIDENCE " + id + " DB_UNCHANGED=true");
        assertEquals(expected, r.getResponse().getStatus(), id + " HTTP status");
        if (expected >= 400) {
            String body = r.getResponse().getContentAsString();
            assertFalse(body.contains("\"accessToken\""), "Must not expose authentication data");
            assertFalse(body.contains("https://meet.google.com/"), "Must not expose a meeting URL");
        }
    }
    long course() {
        // The demo learner already owns the seeded courses. Create a payable fixture,
        // rolled back with the test, instead of changing any existing enrollment.
        return scalar("insert into online_courses(level,total_lessons,total_hours,title,slug,price,status,featured,created_at,updated_at) select level,0,0,'IT pending payable course',?,100000,'PUBLISHED',false,current_timestamp,current_timestamp from online_courses order by id limit 1 returning id", "it-pending-"+java.util.UUID.randomUUID());
    }
    long assignedClass() { return scalar("select id from class_sections where primary_teacher_id=? order by id limit 1", user(TEACHER)); }
    long otherClass() { return scalar("select c.id from class_sections c where c.primary_teacher_id<>? and not exists(select 1 from class_schedules s where s.class_section_id=c.id and s.teacher_id=?) order by c.id limit 1",user(TEACHER),user(TEACHER)); }
    long session(long cid) { return scalar("select id from class_schedules where class_section_id=? order by id limit 1",cid); }
    long unenrolledClass() { return scalar("select c.id from class_sections c where not exists(select 1 from class_enrollments e where e.class_section_id=c.id and e.student_id=?) order by c.id limit 1",user(LEARNER)); }

    @Test void IT_WISHLIST_02() throws Exception {
        db.update("delete from course_list_items where student_id=? and list_type='WISHLIST'",user(LEARNER));
        String before=snapshot("course_list_items");
        MvcResult r=perform("IT_WISHLIST_02",auth(get("/api/student/commerce/wishlist"),LEARNER));
        assertEquals(200,r.getResponse().getStatus()); assertTrue(json(r).isArray()); assertEquals(0,json(r).size());
        assertEquals(before,snapshot("course_list_items"));
    }
    @Test void IT_WISHLIST_03() throws Exception { unchanged("IT_WISHLIST_03",get("/api/student/commerce/wishlist"),403,"course_list_items"); }
    @Test void IT_CART_02() throws Exception {
        long cid=course(); String jwt=token(LEARNER);
        db.update("delete from course_list_items where student_id=? and online_course_id=?",user(LEARNER),cid);
        assertEquals(200,mvc.perform(post("/api/student/commerce/cart/"+cid).header("Authorization",bearer(jwt))).andReturn().getResponse().getStatus(),"Fixture: initial cart add");
        unchanged("IT_CART_02",post("/api/student/commerce/cart/"+cid).header("Authorization",bearer(jwt)),400,"course_list_items");
        assertEquals(1,scalar("select count(*) from course_list_items where student_id=? and online_course_id=? and list_type='CART'",user(LEARNER),cid));
    }
    @Test void IT_CART_03() throws Exception {
        assertEquals(0,scalar("select count(*) from online_courses where id=?",MISSING));
        unchanged("IT_CART_03",auth(post("/api/student/commerce/cart/"+MISSING),LEARNER),400,"course_list_items");
    }
    @Test void IT_CART_04() throws Exception { unchanged("IT_CART_04",post("/api/student/commerce/cart/"+course()),403,"course_list_items"); }
    @Test void IT_CLASS_04() throws Exception {
        assertEquals(0,scalar("select count(*) from class_sections where id=?",MISSING));
        unchanged("IT_CLASS_04",auth(put("/api/staff/classrooms/"+MISSING),STAFF).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"IT Unknown Classroom\",\"deliveryMode\":\"OFFLINE\",\"capacity\":20}"),400,"class_sections","class_schedules");
    }
    @Test void IT_CLASS_05() throws Exception { unchanged("IT_CLASS_05",auth(post("/api/staff/classroom-proposals"),STAFF).contentType(MediaType.APPLICATION_JSON).content("{}"),400,"classroom_proposals"); }
    @Test void IT_ASNTEACH_03() throws Exception {
        // There is no separate teacher-assignment table (classroom_teacher_assignments was dropped
        // in V14__slice10_final_legacy_cleanup.sql). ClassroomTeacherAssignmentRepository is a facade
        // over class_sections.primary_teacher_id, so assignTeacher is an idempotent upsert: re-assigning
        // the already-assigned primary teacher just re-saves the same primary_teacher_id and returns 200,
        // it does not insert a duplicate row or reject the request as a conflict.
        long cid=assignedClass();
        long rowCountBefore=scalar("select count(*) from class_sections");
        MvcResult r=perform("IT_ASNTEACH_03",auth(post("/api/staff/classrooms/"+cid+"/teachers/"+user(TEACHER)+"/assign"),STAFF));
        assertEquals(200,r.getResponse().getStatus(),"IT_ASNTEACH_03 HTTP status");
        assertEquals(rowCountBefore,scalar("select count(*) from class_sections"),"IT_ASNTEACH_03 no class_sections row inserted/deleted");
        assertEquals(user(TEACHER),scalar("select primary_teacher_id from class_sections where id=?",cid),"Primary teacher stays the same, no duplicate assignment created");
    }
    @Test void IT_SCHEDULE_03() throws Exception { unchanged("IT_SCHEDULE_03",auth(get("/api/teacher/classrooms/"+otherClass()+"/sessions"),TEACHER),400,"class_schedules"); }
    @Test void IT_ATTEND_03() throws Exception {
        long cid=otherClass(), sid=session(cid);
        long student=scalar("select student_id from class_enrollments where class_section_id=? and registration_status='ASSIGNED' order by id limit 1",cid);
        unchanged("IT_ATTEND_03",auth(post("/api/teacher/classrooms/attendance"),TEACHER).contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":"+sid+",\"records\":[{\"studentId\":"+student+",\"status\":\"PRESENT\"}]}"),400,"classroom_attendance_records");
    }
    @Test void IT_MNGHW_02() throws Exception { unchanged("IT_MNGHW_02",auth(post("/api/teacher/classrooms/"+assignedClass()+"/homework"),TEACHER).contentType(MediaType.APPLICATION_JSON).content("{}"),400,"classroom_homework"); }
    @Test void IT_MNGHW_03() throws Exception { unchanged("IT_MNGHW_03",auth(post("/api/teacher/classrooms/"+otherClass()+"/homework"),TEACHER).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"IT unauthorized homework\",\"instruction\":\"Write a paragraph\",\"status\":\"OPEN\"}"),400,"classroom_homework"); }
    @Test void IT_TIMETABLE_02() throws Exception { unchanged("IT_TIMETABLE_02",auth(get("/api/student/classrooms/"+unenrolledClass()+"/sessions"),LEARNER),400,"class_schedules"); }
    @Test void IT_TIMETABLE_03() throws Exception { unchanged("IT_TIMETABLE_03",auth(get("/api/student/classrooms/"+MISSING+"/sessions"),LEARNER),400,"class_schedules"); }
    @Test void IT_MATERIAL_03() throws Exception { unchanged("IT_MATERIAL_03",auth(get("/api/student/classrooms/"+MISSING+"/materials"),LEARNER),400,"class_resources"); }
    @Test void IT_HOMEWORK_04() throws Exception {
        long cid=unenrolledClass();
        long hid=scalar("select id from classroom_homework where class_section_id=? order by id limit 1",cid);
        db.update("update classroom_homework set status='OPEN',deadline=current_timestamp+interval '7 days' where id=?",hid);
        unchanged("IT_HOMEWORK_04",auth(post("/api/student/classrooms/homework/"+hid+"/submit"),LEARNER).contentType(MediaType.APPLICATION_JSON).content("{\"textAnswer\":\"Integration test answer\"}"),400,"classroom_homework_submissions");
    }
    @Test void IT_QUIZ_03() throws Exception {
        long cid=unenrolledClass();
        unchanged("IT_QUIZ_03",auth(post("/api/student/classrooms/"+cid+"/practice/"+MISSING+"/attempts"),LEARNER).contentType(MediaType.APPLICATION_JSON).content("{\"answersJson\":\"[]\"}"),400,"classroom_practice_attempt_history");
    }
    @Test void IT_ONLINE_06() throws Exception { unchanged("IT_ONLINE_06",auth(post("/api/content-manager/online-courses"),CM).contentType(MediaType.APPLICATION_JSON).content("{}"),400,"online_courses"); }
    @Test void IT_NOTIF_04() throws Exception {
        long nid=scalar("select id from app_notifications where user_id<>? order by id limit 1",user(LEARNER));
        unchanged("IT_NOTIF_04",auth(patch("/api/student/notifications/"+nid+"/read"),LEARNER),400,"app_notifications");
    }
    long virtualSession(boolean open) {
        long cid=scalar("select c.id from class_sections c join class_enrollments e on e.class_section_id=c.id where c.delivery_mode='VIRTUAL' and e.student_id=? and e.registration_status='ASSIGNED' order by c.id limit 1",user(LEARNER));
        db.update("update class_sections set google_meet_url=?,google_meet_status=? where id=?",open?"https://meet.google.com/it-fixture":null,open?"READY":"NOT_CREATED",cid);
        long sid=session(cid); db.update("update class_schedules set status=? where id=?",open?"OPEN":"SCHEDULED",sid); return sid;
    }
    @Test void IT_GMEET_04() throws Exception { long sid=virtualSession(false); unchanged("IT_GMEET_04",auth(post("/api/student/classrooms/sessions/"+sid+"/join"),LEARNER),409,"classroom_attendance_records"); }
    @Test void IT_GMEET_05() throws Exception { long sid=virtualSession(true); unchanged("IT_GMEET_05",post("/api/student/classrooms/sessions/"+sid+"/join"),403,"classroom_attendance_records"); }

    @Test void IT_CHECKOUT_02() throws Exception {
        long cid=course(); String jwt=token(LEARNER);
        db.update("delete from course_list_items where student_id=?",user(LEARNER));
        assertEquals(200,mvc.perform(post("/api/student/commerce/cart/"+cid).header("Authorization",bearer(jwt))).andReturn().getResponse().getStatus());
        String body="{\"courseIds\":["+cid+"],\"classroomOfferingIds\":[]}";
        MvcResult quote=mvc.perform(post("/api/student/payments/quote").header("Authorization",bearer(jwt)).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();
        assertEquals(200,quote.getResponse().getStatus()); long amount=json(quote).path("totalAmount").asLong(); assertTrue(amount>0);
        long before=scalar("select count(*) from payment_orders");
        boolean enabled=payos.isEnabled(); String client=payos.getClientId(),key=payos.getApiKey(),checksum=payos.getChecksumKey();
        payos.setEnabled(true);payos.setClientId("it-stub");payos.setApiKey("it-stub");payos.setChecksumKey("it-stub-checksum");
        try (MockedConstruction<PayOS> stub=mockConstruction(PayOS.class,(mock,context)-> {
            var requests=mock(vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
            var response=mock(vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse.class);
            when(response.getPaymentLinkId()).thenReturn("it-link");when(response.getCheckoutUrl()).thenReturn("https://pay.example.test/it-link");when(response.getQrCode()).thenReturn("it-qr");
            when(requests.create(any())).thenReturn(response);when(mock.paymentRequests()).thenReturn(requests);
            when(mock.getCrypto()).thenReturn(mock(vn.payos.crypto.CryptoProvider.class));
        })) {
            MvcResult r=perform("IT_CHECKOUT_02",post("/api/student/payments/payos/link").header("Authorization",bearer(jwt)).contentType(MediaType.APPLICATION_JSON).content(body));
            assertEquals(200,r.getResponse().getStatus()); JsonNode response=json(r); assertEquals("it-link",response.path("paymentLinkId").asText()); assertEquals("https://pay.example.test/it-link",response.path("checkoutUrl").asText());
            em.flush();em.clear(); assertEquals(before+1,scalar("select count(*) from payment_orders"));
            long code=response.path("orderCode").asLong(); assertEquals(amount,scalar("select final_amount_vnd from payment_orders where order_code=?",code));
            assertEquals("PENDING",db.queryForObject("select status from payment_orders where order_code=?",String.class,code));
            assertEquals(1,stub.constructed().size());
            System.out.println("WORKBOOK_EVIDENCE IT_CHECKOUT_02 PayOS_SDK_STUB=true DB_ORDER_COUNT_DELTA=1 STATUS=PENDING AMOUNT_MATCH=true");
        } finally { payos.setEnabled(enabled);payos.setClientId(client);payos.setApiKey(key);payos.setChecksumKey(checksum); }
    }
}
