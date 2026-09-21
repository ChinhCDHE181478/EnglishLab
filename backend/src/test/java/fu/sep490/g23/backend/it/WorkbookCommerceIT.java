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

/** Canonical workbook scenarios for Commerce. */
@EnglishLabIT
@RecordApplicationEvents
@Tag("workbook")
public class WorkbookCommerceIT extends WorkbookTestSupport {

    /**
     * Workbook: Commerce!A12
     * Preconditions:
     * Published free course F exists.
     * LEARNER is enrolled (ACTIVE) in F after POST /api/student/online-courses/{F}/register.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/commerce/wishlist/{F}.
     * 2. Query the CourseListItem rows of LEARNER for F.
     * 3. Call GET /api/student/commerce/wishlist.
     * Expected results:
     * The request is rejected with the message that the learner already owns the course.
     * No CourseListItem row is created.
     * F is not in the wishlist.
     */
    @Test
    @DisplayName("IT_COMMERCE_01 — Verify a course the learner already owns cannot be added to the wishlist.")
    void IT_COMMERCE_01() throws Exception {
        String u=freshLearner();long c=course(0,1);enroll(u,c);var r=rejected("POST","/api/student/commerce/wishlist/"+c,u,null);assertTrue(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("sở hữu"));assertEquals(0,n("select count(*) from course_list_items where student_id=? and online_course_id=?",uid(u),c));assertTrue(ok("GET","/api/student/commerce/wishlist",u,null).isEmpty());
    }

    /**
     * Workbook: Commerce!A15
     * Preconditions:
     * Published free course F exists.
     * LEARNER is not enrolled in F.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/commerce/cart/{F}.
     * 2. Query the CourseListItem rows of LEARNER for F and call GET /api/student/commerce/cart.
     * Expected results:
     * The request is rejected with the message that a free course must be registered directly.
     * No CART row is created and the cart is empty.
     */
    @Test
    @DisplayName("IT_COMMERCE_03 — Verify a free course cannot be added to the cart.")
    void IT_COMMERCE_03() throws Exception {
        String u=freshLearner();long c=course(0,1);rejected("POST","/api/student/commerce/cart/"+c,u,null);assertEquals(0,n("select count(*) from course_list_items where student_id=?",uid(u)));assertTrue(ok("GET","/api/student/commerce/cart",u,null).isEmpty());
    }

    /**
     * Workbook: Commerce!A18
     * Preconditions:
     * Published paid courses X and Y exist; id 999999991 does not exist.
     * The cart of LEARNER is empty.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/commerce/cart/sync with {"courseIds":[X,Y,999999991,X]}.
     * 2. Query the CourseListItem rows of type CART for LEARNER and call GET /api/student/commerce/cart.
     * Expected results:
     * The request succeeds and returns the current cart.
     * CART contains X and Y once each and nothing for the unknown id.
     */
    @Test
    @DisplayName("IT_COMMERCE_06 — Verify syncing a guest cart merges valid courses once and skips unavailable ones.")
    void IT_COMMERCE_06() throws Exception {
        String u=freshLearner();long x=course(100000,1),y=course(100000,1);var r=ok("POST","/api/student/commerce/cart/sync",u,body("courseIds",List.of(x,y,999999991L,x)));assertEquals(2,r.size());assertFalse(row(r,"id",x).isMissingNode()&&row(r,"courseId",x).isMissingNode());assertEquals(2,n("select count(*) from course_list_items where student_id=? and list_type='CART'",uid(u)));assertEquals(2,ok("GET","/api/student/commerce/cart",u,null).size());
    }

    /**
     * Workbook: Commerce!A21
     * Preconditions:
     * LEARNER has paid courses X and Y in the cart and paid course Z in the wishlist.
     * Procedure:
     * 1. Login as LEARNER and call DELETE /api/student/commerce/cart.
     * 2. Query the CourseListItem rows of LEARNER and call GET /api/student/commerce/cart and GET /api/student/commerce/wishlist.
     * Expected results:
     * All CART rows of LEARNER are deleted and the cart is empty.
     * The WISHLIST row of Z is unchanged.
     */
    @Test
    @DisplayName("IT_COMMERCE_08 — Verify clearing the cart deletes all cart rows and keeps the wishlist.")
    void IT_COMMERCE_08() throws Exception {
        String u=freshLearner();long x=course(100000,1),y=course(100000,1),z=course(100000,1);ok("POST","/api/student/commerce/cart/"+x,u,null);ok("POST","/api/student/commerce/cart/"+y,u,null);ok("POST","/api/student/commerce/wishlist/"+z,u,null);long wid=n("select id from course_list_items where student_id=? and list_type='WISHLIST'",uid(u));ok("DELETE","/api/student/commerce/cart",u,null);assertEquals(0,n("select count(*) from course_list_items where student_id=? and list_type='CART'",uid(u)));assertEquals(wid,n("select id from course_list_items where student_id=?",uid(u)));assertTrue(ok("GET","/api/student/commerce/cart",u,null).isEmpty());assertEquals(1,ok("GET","/api/student/commerce/wishlist",u,null).size());
    }

    /**
     * Workbook: Commerce!A26
     * Preconditions:
     * Fresh learner; published paid course C with one lesson. PayOS SDK including crypto is stubbed. Price change is test-fixture SQL, not a Content Manager API operation.
     * Procedure:
     * 1. Add published course C (100000 VND) to the learner cart and POST /api/student/payments/quote.
     * 2. Simulate a concurrent price change to 130000 VND in the isolated database fixture.
     * 3. Request a fresh quote.
     * 4. POST /api/student/payments/payos/link with courseIds [C]; inspect payment_orders.
     * Expected results:
     * The first quote is 100000 VND and the refreshed quote is 130000 VND. Exactly one new PENDING order is created with final_amount_vnd 130000. PayOS link creation uses a stub.
     */
    @Test
    @DisplayName("IT_COMMERCE_11 — Verify refreshing the quote after a price change creates an order at the current price.")
    void IT_COMMERCE_11() throws Exception {
        String u=freshLearner();long c=course(100000,1);ok("POST","/api/student/commerce/cart/"+c,u,null);
         assertEquals(100000,ok("POST","/api/student/payments/quote",u,body("courseIds",List.of(c))).path("totalAmount").asLong());
         sql("update online_courses set price=130000 where id=?",c);
         assertEquals(130000,ok("POST","/api/student/payments/quote",u,body("courseIds",List.of(c))).path("totalAmount").asLong());
         long before=n("select count(*) from payment_orders where student_id=?",uid(u));
         try(var stub=provider(false)){var link=ok("POST","/api/student/payments/payos/link",u,body("courseIds",List.of(c)));long code=link.path("orderCode").asLong();
         assertEquals("PENDING",orderStatus(code));assertEquals(130000,n("select final_amount_vnd from payment_orders where order_code=?",code));assertEquals(before+1,n("select count(*) from payment_orders where student_id=?",uid(u)));}
    }

    /**
     * Workbook: Commerce!A28
     * Preconditions:
     * PENDING PaymentOrder O exists for LEARNER and paid course X.
     * Procedure:
     * 1. Send POST /api/payos/webhook for O with a tampered signature.
     * 2. Query O and the OnlineCourseEnrollment rows for LEARNER and X.
     * 3. Login as LEARNER and call GET /api/student/online-courses/{X}/content.
     * Expected results:
     * The webhook is rejected with the invalid-signature message.
     * O is still PENDING and no OnlineCourseEnrollment exists.
     * The content request is rejected with 403.
     */
    @Test
    @DisplayName("IT_COMMERCE_13 — Verify a PayOS webhook with a wrong signature leaves the order pending.")
    void IT_COMMERCE_13() throws Exception {
        String u=freshLearner();long c=course(100000,1);try(var stub=provider(false)){long code=order(u,c,null);var r=rejected("POST","/api/payos/webhook",null,body("code","00","success",true,"signature","tampered","data",Map.of("orderCode",code,"code","00")));assertTrue(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8).contains("Chữ ký"));assertEquals("PENDING",orderStatus(code));assertEquals(0,n("select count(*) from online_course_enrollments where student_id=? and online_course_id=?",uid(u),c));status(call("GET",content(c),u,null),403);}
    }

    /**
     * Workbook: Commerce!A29
     * Preconditions:
     * PENDING PaymentOrder O exists for LEARNER and paid course X.
     * LEARNER has X in the cart.
     * Procedure:
     * 1. Send a signed POST /api/payos/webhook for O with data.code "01".
     * 2. Query O and the OnlineCourseEnrollment and CART rows of LEARNER for X.
     * 3. Login as LEARNER and call GET /api/student/online-courses/{X}/content.
     * Expected results:
     * O has status FAILED.
     * No OnlineCourseEnrollment exists and the CART row of X is kept.
     * The content request is rejected with 403.
     */
    @Test
    @DisplayName("IT_COMMERCE_14 — Verify a failed PayOS payment gives no access and keeps the course in the cart.")
    void IT_COMMERCE_14() throws Exception {
        String u=freshLearner();long c=course(100000,1);try(var stub=provider(false)){long code=order(u,c,null);webhook(code,false);assertEquals("FAILED",orderStatus(code));assertEquals(0,n("select count(*) from online_course_enrollments where student_id=? and online_course_id=?",uid(u),c));assertEquals(1,n("select count(*) from course_list_items where student_id=? and online_course_id=? and list_type='CART'",uid(u),c));status(call("GET",content(c),u,null),403);}
    }

    /**
     * Workbook: Commerce!A30
     * Preconditions:
     * CM account exists; published paid course X with price 100000 exists.
     * LEARNER has X in the cart.
     * Procedure:
     * 1. Login as CM and call POST /api/content-manager/discount-codes with code CP10, name "Ten percent", type PERCENTAGE, value 10 and usageLimit 5.
     * 2. Login as LEARNER and call POST /api/student/payments/quote with courseIds [X] and couponCode CP10.
     * 3. Call POST /api/student/payments/payos/link with couponCode CP10 and the displayed amounts.
     * 4. As CM call GET /api/content-manager/discount-codes and read CP10.
     * 5. Send the signed success webhook for the order.
     * 6. As CM call GET /api/content-manager/discount-codes and read CP10 again.
     * Expected results:
     * The quote shows couponDiscountAmount 10000 and totalAmount 90000.
     * The PaymentOrder has amount 90000 and references the coupon.
     * After link creation CP10 has usedCount 0, reservedCount 1 and remainingUses 4.
     * After the successful webhook CP10 has usedCount 1, reservedCount 0 and remainingUses 4.
     */
    @Test
    @DisplayName("IT_COMMERCE_15 — Verify a discount code is reserved when the payment link is created and consumed after payment.")
    void IT_COMMERCE_15() throws Exception {
        String u=freshLearner();long c=course(100000,1);String cp=coupon();var q=ok("POST","/api/student/payments/quote",u,body("courseIds",List.of(c),"couponCode",cp));assertEquals(10000,q.path("couponDiscountAmount").asLong());assertEquals(90000,q.path("totalAmount").asLong());try(var stub=provider(false)){long code=order(u,c,cp);assertEquals(90000,n("select final_amount_vnd from payment_orders where order_code=?",code));couponCounts(cp,0,1,4);webhook(code,true);couponCounts(cp,1,0,4);}
    }

    /**
     * Workbook: Commerce!A31
     * Preconditions:
     * Discount code CP10 exists with usageLimit 5.
     * LEARNER created a PENDING order O with CP10, so CP10 has reservedCount 1.
     * CM account exists.
     * Procedure:
     * 1. Send a signed POST /api/payos/webhook for O with data.code "01".
     * 2. As CM call GET /api/content-manager/discount-codes and read CP10.
     * Expected results:
     * O has status FAILED.
     * CP10 has reservedCount 0, usedCount 0 and remainingUses 5.
     */
    @Test
    @DisplayName("IT_COMMERCE_16 — Verify a failed payment releases the reserved discount code.")
    void IT_COMMERCE_16() throws Exception {
        String u=freshLearner();long c=course(100000,1);String cp=coupon();try(var stub=provider(false)){long code=order(u,c,cp);couponCounts(cp,0,1,4);webhook(code,false);assertEquals("FAILED",orderStatus(code));couponCounts(cp,0,0,5);}
    }

    /**
     * Workbook: Commerce!A32
     * Preconditions:
     * PENDING PaymentOrder O exists for LEARNER and paid course X.
     * PayOS is enabled and the stubbed PayOS client reports the payment link of O as PAID.
     * No webhook was delivered for O.
     * Procedure:
     * 1. Run the payment reconciliation job.
     * 2. Query O and the OnlineCourseEnrollment rows for LEARNER and X.
     * 3. Login as LEARNER and call GET /api/student/online-courses/{X}/content.
     * Expected results:
     * O is PAID with paidAt set.
     * Exactly one OnlineCourseEnrollment(LEARNER, X) with status ACTIVE exists.
     * The learner receives the lesson content of X.
     */
    @Test
    @DisplayName("IT_COMMERCE_17 — Verify the reconciliation job settles an order whose webhook never arrived.")
    void IT_COMMERCE_17() throws Exception {
        String u=freshLearner();long c=course(100000,1);try(var stub=provider(true)){long code=order(u,c,null);payments.reconcilePendingPaymentOrders();em.flush();em.clear();assertEquals("PAID",orderStatus(code));assertNotNull(str("select paid_at::text from payment_orders where order_code=?",code));assertEquals(1,n("select count(*) from online_course_enrollments where student_id=? and online_course_id=? and status='ACTIVE'",uid(u),c));assertFalse(ok("GET",content(c),u,null).path("modules").isEmpty());}
    }

    /**
     * Workbook: Commerce!A34
     * Preconditions:
     * Published free course F (price 0) with one module and one lesson exists.
     * LEARNER is not enrolled in F.
     * Procedure:
     * 1. Login as LEARNER and call GET /api/student/online-courses/{F}/content.
     * 2. Call POST /api/student/online-courses/{F}/register.
     * 3. Query the OnlineCourseEnrollment rows for LEARNER and F.
     * 4. Call GET /api/student/online-courses/my-enrollments and GET /api/student/online-courses/{F}/content again.
     * Expected results:
     * Before registration the content request is rejected with 403 and no lesson data is returned.
     * Exactly one OnlineCourseEnrollment(LEARNER, F) with status ACTIVE is created.
     * The enrollment list contains F.
     * After registration the content request returns the published modules and lessons of F.
     */
    @Test
    @DisplayName("IT_COMMERCE_18 — Verify registering a free course creates the enrollment that unlocks the lessons.")
    void IT_COMMERCE_18() throws Exception {
        String u=freshLearner();long c=course(0,1);status(call("GET",content(c),u,null),403);enroll(u,c);assertEquals(1,n("select count(*) from online_course_enrollments where student_id=? and online_course_id=? and status='ACTIVE'",uid(u),c));assertFalse(ok("GET",content(c),u,null).path("modules").isEmpty());assertEquals(1,ok("GET","/api/student/online-courses/my-enrollments",u,null).size());
    }

    /**
     * Workbook: Commerce!A35
     * Preconditions:
     * Published paid course P exists.
     * LEARNER is not enrolled in P.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/online-courses/{P}/register.
     * 2. Query the OnlineCourseEnrollment rows for LEARNER and P.
     * 3. Call GET /api/student/online-courses/{P}/content and GET /api/student/online-courses/my-enrollments.
     * Expected results:
     * The registration is rejected with the message that a paid course is activated only after successful payment.
     * No OnlineCourseEnrollment row exists for LEARNER and P.
     * The content request is rejected with 403 and P is not in the enrollment list.
     */
    @Test
    @DisplayName("IT_COMMERCE_19 — Verify a paid course cannot be self-registered without payment.")
    void IT_COMMERCE_19() throws Exception {
        String u=freshLearner();long c=course(100000,1);rejected("POST","/api/student/online-courses/"+c+"/register",u,null);assertEquals(0,n("select count(*) from online_course_enrollments where student_id=? and online_course_id=?",uid(u),c));status(call("GET",content(c),u,null),403);assertTrue(ok("GET","/api/student/online-courses/my-enrollments",u,null).isEmpty());
    }

    /**
     * Workbook: Commerce — Add Course to Wishlist
     * Preconditions:
     * Published paid course X exists.
     * LEARNER is not enrolled in X and X is not in the wishlist.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/commerce/wishlist/{X}.
     * 2. Query the CourseListItem rows of LEARNER for X.
     * 3. Call GET /api/student/commerce/wishlist.
     * Expected results:
     * Exactly one WISHLIST row is created for LEARNER and X.
     * The wishlist returns X exactly once.
     */
    @Test
    @DisplayName("IT_COMMERCE_20 — Verify adding a course to the wishlist persists and appears in the list.")
    void IT_COMMERCE_20() throws Exception {
        String u=freshLearner();long c=course(100000,1);ok("POST","/api/student/commerce/wishlist/"+c,u,null);assertEquals(1,n("select count(*) from course_list_items where student_id=? and online_course_id=? and list_type='WISHLIST'",uid(u),c));assertEquals(1,ok("GET","/api/student/commerce/wishlist",u,null).size());
    }

    /**
     * Workbook: Commerce — Move Course from Wishlist to Cart
     * Preconditions:
     * Published paid course X exists.
     * LEARNER has an empty wishlist and cart; X is NOT in the wishlist.
     * Procedure:
     * 1. Login as LEARNER and call POST /api/student/commerce/wishlist/{X}/move-to-cart.
     * 2. Query the CourseListItem rows of LEARNER for X.
     * Expected results:
     * The request is rejected.
     * No CourseListItem row exists for LEARNER and X.
     */
    @Test
    @DisplayName("IT_COMMERCE_21 — Verify moving a course not in the wishlist to the cart is rejected.")
    void IT_COMMERCE_21() throws Exception {
        String u=freshLearner();long c=course(100000,1);rejected("POST","/api/student/commerce/wishlist/"+c+"/move-to-cart",u,null);assertEquals(0,n("select count(*) from course_list_items where student_id=? and online_course_id=?",uid(u),c));
    }
}
