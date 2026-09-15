# Bộ dữ liệu mẫu nhập tay cho Content Manager

Tài liệu này bám theo các biểu mẫu hiện có trong code. Mỗi mục cung cấp một bản ghi mẫu để nhập thủ công, không phải script seed và không gọi API trực tiếp.

## Cách sử dụng

1. Tạo dữ liệu theo đúng thứ tự trong tài liệu để các danh sách liên kết có đủ lựa chọn.
2. Với ảnh, audio, video và tài liệu, dùng nút tải tệp của giao diện. Không nhập URL giả.
3. Ban đầu nên lưu ở trạng thái **Bản nháp**. Chỉ chuyển sang **Đã xuất bản** sau khi đã xem trước.
4. Nếu mã `IELTS` hoặc dữ liệu cùng tên đã tồn tại, chỉnh sửa bản ghi hiện có hoặc thêm hậu tố `-MANUAL-01` vào mã.

---

## 1. Danh mục khóa học

Đường dẫn: `/content-manager/categories`

| Trường | Giá trị nhập |
|---|---|
| Mã danh mục | `IELTS` |
| Tên hiển thị | `Luyện thi IELTS` |
| Trạng thái | `Đang hoạt động` |
| Mô tả | `Các khóa học phát triển toàn diện bốn kỹ năng IELTS, từ nền tảng đến mục tiêu Band 6.5+.` |

Nếu mã `IELTS` đã tồn tại, không tạo trùng; dùng bản ghi hiện có cho các bước sau.

---

## 2. Bộ tiêu chí chấm điểm Writing

Đường dẫn: `/content-manager/rubrics`

### Thông tin chung

| Trường | Giá trị nhập |
|---|---|
| Tên bộ tiêu chí | `IELTS Writing Task 2 - Band 0-9` |
| Loại kỳ thi | `IELTS` |
| Kỹ năng | `Writing` |
| Dạng bài | `Writing Task 2` |
| Thang điểm | `IELTS Band 0-9` |
| Trạng thái | `Đang dùng` |
| Mô tả | `Đánh giá bài luận IELTS Writing Task 2 theo bốn tiêu chí chính thức, mỗi tiêu chí chiếm 25%.` |

### Tiêu chí 1

| Trường | Giá trị nhập |
|---|---|
| Tên tiêu chí | `Task Response` |
| Trọng số | `25` |
| Thứ tự | `1` |
| Mô tả | `Mức độ trả lời đầy đủ yêu cầu đề, phát triển lập luận và sử dụng ví dụ phù hợp.` |
| Mô tả band chi tiết | `Band 5: Trả lời một phần yêu cầu, ý còn hạn chế. Band 6: Trả lời đầy đủ nhưng một số ý chưa phát triển tốt. Band 7: Trả lời toàn diện, lập trường rõ và ý được mở rộng hợp lý. Band 8-9: Lập luận sâu, chính xác và thuyết phục.` |

### Tiêu chí 2

| Trường | Giá trị nhập |
|---|---|
| Tên tiêu chí | `Coherence and Cohesion` |
| Trọng số | `25` |
| Thứ tự | `2` |
| Mô tả | `Cách tổ chức đoạn văn, phát triển mạch ý và sử dụng phương tiện liên kết.` |
| Mô tả band chi tiết | `Band 5: Tổ chức chưa ổn định. Band 6: Mạch ý nhìn chung rõ nhưng liên kết đôi lúc máy móc. Band 7: Bố cục hợp lý, mỗi đoạn có trọng tâm. Band 8-9: Lập luận liền mạch và liên kết tự nhiên.` |

### Tiêu chí 3

| Trường | Giá trị nhập |
|---|---|
| Tên tiêu chí | `Lexical Resource` |
| Trọng số | `25` |
| Thứ tự | `3` |
| Mô tả | `Phạm vi từ vựng, độ chính xác, khả năng diễn đạt lại và dùng từ theo ngữ cảnh.` |
| Mô tả band chi tiết | `Band 5: Vốn từ hạn chế và lặp từ. Band 6: Đủ dùng cho chủ đề nhưng còn lỗi chọn từ. Band 7: Từ vựng đa dạng, có khả năng diễn đạt linh hoạt. Band 8-9: Dùng từ chính xác, tự nhiên và tinh tế.` |

### Tiêu chí 4

| Trường | Giá trị nhập |
|---|---|
| Tên tiêu chí | `Grammatical Range and Accuracy` |
| Trọng số | `25` |
| Thứ tự | `4` |
| Mô tả | `Mức độ đa dạng cấu trúc câu và độ chính xác ngữ pháp, dấu câu.` |
| Mô tả band chi tiết | `Band 5: Cấu trúc đơn giản, lỗi thường xuyên. Band 6: Có câu phức nhưng độ chính xác chưa ổn định. Band 7: Nhiều cấu trúc đa dạng, phần lớn câu đúng. Band 8-9: Kiểm soát ngữ pháp linh hoạt với rất ít lỗi.` |

---

## 3. Bộ tiêu chí chấm điểm Speaking

Đường dẫn: `/content-manager/rubrics`

### Thông tin chung

| Trường | Giá trị nhập |
|---|---|
| Tên bộ tiêu chí | `IELTS Speaking - Band 0-9` |
| Loại kỳ thi | `IELTS` |
| Kỹ năng | `Speaking` |
| Dạng bài | `IELTS Speaking Full Test` |
| Thang điểm | `IELTS Band 0-9` |
| Trạng thái | `Đang dùng` |
| Mô tả | `Đánh giá bài nói IELTS theo độ trôi chảy, từ vựng, ngữ pháp và phát âm.` |

### Bốn tiêu chí

| Tên tiêu chí | Trọng số | Thứ tự | Mô tả | Mô tả band chi tiết |
|---|---:|---:|---|---|
| `Fluency and Coherence` | `25` | `1` | `Khả năng nói liên tục, phát triển câu trả lời và liên kết ý.` | `Band 5: Ngập ngừng và lặp ý. Band 6: Duy trì được câu trả lời nhưng đôi lúc mất mạch. Band 7: Nói dài, rõ và ít ngập ngừng. Band 8-9: Trôi chảy, mạch lạc và phát triển ý tự nhiên.` |
| `Lexical Resource` | `25` | `2` | `Phạm vi từ vựng, độ chính xác và khả năng diễn đạt lại.` | `Band 5: Từ vựng hạn chế. Band 6: Đủ diễn đạt chủ đề quen thuộc. Band 7: Linh hoạt với nhiều chủ đề. Band 8-9: Chính xác, thành thạo và tự nhiên.` |
| `Grammatical Range and Accuracy` | `25` | `3` | `Độ đa dạng và chính xác của cấu trúc ngữ pháp khi nói.` | `Band 5: Chủ yếu dùng câu đơn. Band 6: Kết hợp câu đơn và phức với một số lỗi. Band 7: Nhiều cấu trúc đúng. Band 8-9: Linh hoạt và gần như không có lỗi gây cản trở.` |
| `Pronunciation` | `25` | `4` | `Độ rõ, trọng âm, ngữ điệu và mức độ dễ hiểu.` | `Band 5: Người nghe phải tập trung. Band 6: Nhìn chung dễ hiểu. Band 7: Phát âm rõ với ngữ điệu phù hợp. Band 8-9: Dễ hiểu, tự nhiên và kiểm soát tốt đặc điểm phát âm.` |

---

## 4. Kho học liệu

Đường dẫn: `/content-manager/materials`

| Trường | Giá trị nhập |
|---|---|
| Tên học liệu | `IELTS Writing Task 2 - Opinion Essay Planner` |
| Mô tả | `Phiếu lập dàn ý giúp học viên xác định lập trường, xây dựng hai luận điểm và chuẩn bị ví dụ trước khi viết bài.` |
| Tệp học liệu | Tải lên một tệp PDF bằng khu vực kéo thả |
| Liên kết tệp | Để hệ thống tự điền sau khi tải tệp thành công |
| Kỹ năng chính | `Viết` |
| Kỳ thi | `IELTS` |
| IELTS Band tối thiểu | `5.0` |
| IELTS Band tối đa | `7.0` |
| Nhãn gợi ý | `writing task 2, opinion essay, lập dàn ý` |
| Trạng thái | `Đã xuất bản` |

---

## 5. Thẻ ghi nhớ

Đường dẫn: `/content-manager/flashcards`

### Thông tin bộ thẻ

| Trường | Giá trị nhập |
|---|---|
| Tên bộ thẻ | `IELTS Writing - Education Vocabulary` |
| Mô tả | `Từ vựng học thuật dùng để thảo luận về giáo dục trong IELTS Writing Task 2.` |
| Nhãn gợi ý | `education, writing task 2, band 6.5` |
| Danh mục | `IELTS` |
| Trạng thái | `Đã xuất bản` |

### Thẻ 1

| Trường | Giá trị nhập |
|---|---|
| Thuật ngữ | `compulsory education` |
| Định nghĩa | `giáo dục bắt buộc` |
| Ví dụ | `Compulsory education ensures that every child receives basic knowledge and essential skills.` |
| Lỗi thường gặp | `Không dùng “obligatory education” trong ngữ cảnh học thuật thông thường.` |

### Thẻ 2

| Trường | Giá trị nhập |
|---|---|
| Thuật ngữ | `academic achievement` |
| Định nghĩa | `thành tích học tập` |
| Ví dụ | `Parental support can have a positive effect on children's academic achievement.` |
| Lỗi thường gặp | `Không viết “academic success result”; dùng trực tiếp “academic achievement”.` |

### Thẻ 3

| Trường | Giá trị nhập |
|---|---|
| Thuật ngữ | `equal access to education` |
| Định nghĩa | `cơ hội tiếp cận giáo dục bình đẳng` |
| Ví dụ | `Governments should provide equal access to education for students in rural areas.` |
| Lỗi thường gặp | `Dùng “access to”, không dùng “access for education”.` |

### Thẻ 4

| Trường | Giá trị nhập |
|---|---|
| Thuật ngữ | `vocational training` |
| Định nghĩa | `đào tạo nghề` |
| Ví dụ | `Vocational training can prepare young people for the demands of the labour market.` |
| Lỗi thường gặp | `Không đồng nhất hoàn toàn “vocational training” với “university education”.` |

---

## 6. Ngân hàng bài tập

Đường dẫn: `/content-manager/exercise-bank`

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `Lập dàn ý Opinion Essay về giáo dục đại học` |
| Kỹ năng | `Writing` |
| Loại bài | `Bài tập về nhà` |
| Cấp độ | `IELTS Band 5.5-6.5` |
| Đề bài | `Some people believe that university education should be free for everyone. To what extent do you agree or disagree? Hãy lập dàn ý gồm mở bài, hai đoạn thân bài và kết luận.` |
| Đáp án / tiêu chí chấm | `Có lập trường rõ ràng; mỗi đoạn thân bài có một topic sentence, phần giải thích và ví dụ; các ý trực tiếp trả lời câu hỏi.` |
| Giải thích | `Một dàn ý tốt cần thể hiện quan điểm nhất quán. Không liệt kê quá nhiều ý; ưu tiên hai luận điểm có thể giải thích và minh họa rõ.` |
| Thẻ | `writing, task 2, education, outline` |
| Trạng thái | `Đang dùng` |

---

## 7. Luyện nghe

Đường dẫn: `/content-manager/listening`

### Thông tin chung

| Trường | Giá trị nhập |
|---|---|
| Tên bài nghe | `IELTS Listening Mini Practice - Library Registration` |
| Mô tả | `Bài nghe ngắn về cuộc hội thoại đăng ký thẻ thư viện, luyện nhận diện tên riêng, ngày tháng và thông tin chi tiết.` |
| Trạng thái | `Đã xuất bản` |
| Hướng dẫn làm bài | `Nghe đoạn hội thoại và chọn một đáp án đúng cho mỗi câu hỏi. Chỉ nghe tối đa hai lần.` |
| Điểm đạt | `2` |
| Điểm tối đa | `3` |
| Thời lượng | `8` |

### Nội dung biên soạn

| Trường | Giá trị nhập |
|---|---|
| Tên phần | `Section 1 - Library Registration` |
| Tóm tắt | `Một sinh viên đăng ký sử dụng thư viện thành phố.` |
| Audio | Tải lên tệp MP3 do bạn chuẩn bị |
| Transcript | `Librarian: Good morning. How can I help you? Student: I'd like to register for a library card. Librarian: Certainly. Can I have your full name? Student: It's Daniel Harper. Librarian: And your date of birth? Student: The twelfth of May, nineteen ninety-eight. Librarian: Thank you. The annual fee is fifteen pounds.` |
| Tên nhóm câu hỏi | `Questions 1-3` |
| Dạng câu hỏi | `Một đáp án` |

### Câu hỏi và đáp án

| Câu | Nội dung | A | B | C | D | Đáp án | Giải thích |
|---:|---|---|---|---|---|---|---|
| `1` | `What is the student's surname?` | `Harris` | `Harper` | `Parker` | `Carter` | `B` | `The student says his full name is Daniel Harper.` |
| `2` | `When was the student born?` | `2 May 1998` | `12 March 1998` | `12 May 1998` | `20 May 1989` | `C` | `He says “the twelfth of May, nineteen ninety-eight”.` |
| `3` | `How much is the annual fee?` | `£5` | `£10` | `£15` | `£50` | `C` | `The librarian states that the annual fee is fifteen pounds.` |

---

## 8. Luyện đọc

Đường dẫn: `/content-manager/reading`

### Thông tin chung

| Trường | Giá trị nhập |
|---|---|
| Tên bài đọc | `IELTS Reading Mini Practice - Urban Green Spaces` |
| Mô tả | `Bài đọc ngắn về lợi ích của không gian xanh trong đô thị.` |
| Trạng thái | `Đã xuất bản` |
| Hướng dẫn làm bài | `Đọc bài văn và chọn đáp án đúng nhất cho mỗi câu hỏi.` |
| Điểm đạt | `2` |
| Điểm tối đa | `3` |
| Thời lượng | `10` |

### Bài đọc

| Trường | Giá trị nhập |
|---|---|
| Tên phần | `Passage 1` |
| Tên nhóm câu hỏi | `Questions 1-3` |
| Dạng câu hỏi | `Một đáp án` |
| Nội dung bài đọc | `Urban green spaces such as parks and community gardens provide benefits that extend beyond recreation. Trees reduce surface temperatures by offering shade and releasing moisture into the air. Green areas can also improve mental well-being because they give residents a place to exercise, relax and meet other people. However, these benefits are not distributed equally. In many cities, wealthier neighbourhoods have more accessible and better-maintained parks than lower-income areas. Urban planners are therefore increasingly using measures of accessibility, not simply total green area, when designing healthier cities.` |

### Câu hỏi và đáp án

| Câu | Nội dung | A | B | C | D | Đáp án | Giải thích |
|---:|---|---|---|---|---|---|---|
| `1` | `How do trees help reduce urban temperatures?` | `They block traffic.` | `They provide shade and release moisture.` | `They increase rainfall.` | `They reflect all sunlight.` | `B` | `The first part states both mechanisms directly.` |
| `2` | `Why can green spaces improve mental well-being?` | `They reduce housing costs.` | `They create more office space.` | `They provide places to exercise, relax and socialise.` | `They remove the need for transport.` | `C` | `The passage lists exercise, relaxation and meeting others.` |
| `3` | `What are planners increasingly measuring?` | `Accessibility to green spaces` | `The age of city trees` | `The cost of community gardens` | `Annual rainfall` | `A` | `The final sentence contrasts accessibility with total green area.` |

---

## 9. Luyện viết

Đường dẫn: `/content-manager/writing`

| Trường | Giá trị nhập |
|---|---|
| Tên đề viết | `IELTS Writing Task 2 - Free University Education` |
| Mô tả | `Bài luyện Opinion Essay về chi phí giáo dục đại học.` |
| Trạng thái | `Đã xuất bản` |
| Bộ tiêu chí chấm | `IELTS Writing Task 2 - Band 0-9` |
| Hướng dẫn làm bài | `Viết ít nhất 250 từ. Trình bày lập trường rõ ràng và hỗ trợ quan điểm bằng lý do cùng ví dụ phù hợp.` |
| Điểm đạt | `6.0` |
| Điểm tối đa | `9.0` |
| Thời lượng | `40` |
| Loại task | `Task 2` |
| Tiêu đề task | `Education and public funding` |
| Đề bài | `Some people believe that university education should be free for everyone, regardless of personal income. To what extent do you agree or disagree?` |
| Số từ tối thiểu | `250` |
| Thời gian gợi ý | `40` |
| Bài mẫu | `Higher education creates benefits for both individuals and society, but making every university course completely free is not necessarily the fairest or most sustainable solution. I partly agree that governments should remove financial barriers, although students who can afford to contribute should still pay a reasonable share. Public funding is essential for capable students from low-income families. Without scholarships or subsidised tuition, many of them may abandon university despite having strong academic potential. This wastes talent and can deepen social inequality. Governments should therefore guarantee that financial hardship never prevents a qualified student from studying. However, universal free tuition would require substantial public spending and could direct money away from schools, healthcare or vocational training. A more balanced policy would combine income-based grants with affordable fees and repayment systems linked to graduates' future earnings. In conclusion, access to university should not depend on wealth, but targeted support is preferable to making every course free for every student.` |

---

## 10. Luyện nói

Đường dẫn: `/content-manager/speaking`

| Trường | Giá trị nhập |
|---|---|
| Tên đề nói | `IELTS Speaking Practice - Education and Learning` |
| Mô tả | `Bài luyện đủ ba phần về trường học, một kỹ năng đã học và vai trò của giáo dục.` |
| Trạng thái | `Đã xuất bản` |
| Bộ tiêu chí chấm | `IELTS Speaking - Band 0-9` |
| Hướng dẫn làm bài | `Trả lời bằng tiếng Anh và ghi âm trong từng phần. Cố gắng mở rộng câu trả lời bằng lý do hoặc ví dụ.` |
| Điểm đạt | `6.0` |
| Điểm tối đa | `9.0` |
| Thời lượng | `14` |

### Part 1

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `School and study` |
| Câu hỏi 1 | `What subject did you enjoy most at school?` |
| Câu hỏi 2 | `Do you prefer studying alone or with other people?` |
| Câu hỏi 3 | `What would you like to learn in the future?` |

### Part 2

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `Describe a useful skill you learned` |
| Thẻ chủ đề | `Describe a useful skill you learned. You should say what the skill is, when and how you learned it, why it is useful, and explain how you felt after learning it.` |
| Thời gian chuẩn bị | `60` giây |
| Thời gian trả lời | `120` giây |

### Part 3

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `Education in society` |
| Câu hỏi 1 | `What skills should schools teach in addition to academic subjects?` |
| Câu hỏi 2 | `How has technology changed the way people learn?` |
| Câu hỏi 3 | `Should adults continue studying throughout their careers?` |

---

## 11. Ngân hàng đề thi thử

Đường dẫn: `/content-manager/mock-exams`

Tạo một đề nhỏ để kiểm tra luồng trước khi nhập đề đủ số câu.

| Trường | Giá trị nhập |
|---|---|
| Tên đề | `TOEIC Listening Mini Test 01` |
| Mô tả | `Đề TOEIC Listening rút gọn gồm ba câu để kiểm tra cấu trúc, audio và chấm đáp án.` |
| Kỳ thi | `TOEIC` |
| Kỹ năng | `Listening` |
| Trạng thái | `Đã xuất bản` |
| Hướng dẫn làm bài | `Listen to each recording and choose the best answer.` |
| Điểm đạt | `2` |
| Điểm tối đa | `3` |
| Thời lượng | `8` |
| Tên phần | `Part 2 - Question-Response` |
| Tên nhóm câu hỏi | `Questions 1-3` |
| Audio | Tải lên audio TOEIC mẫu của bạn |

| Câu | Nội dung | A | B | C | D | Đáp án |
|---:|---|---|---|---|---|---|
| `1` | `When will the meeting begin?` | `In the main office.` | `At half past nine.` | `With the sales team.` | `It was productive.` | `B` |
| `2` | `Who ordered these office chairs?` | `Ms. Delgado did.` | `They are very comfortable.` | `On the second floor.` | `About twenty chairs.` | `A` |
| `3` | `Could you send me the revised report?` | `The printer is new.` | `Yes, I'll email it this afternoon.` | `The report was long.` | `In the blue folder.` | `B` |

---

## 12. Bài đánh giá đầu vào

Đường dẫn: `/content-manager/placement-test`

Đây là cấu hình dùng chung, không tạo nhiều bản ghi. Chỉnh từng tab rồi bấm **Lưu toàn bộ thay đổi**.

### Trạng thái chung

| Trường | Giá trị nhập |
|---|---|
| Cho phép học viên làm bài | `Bật` sau khi hoàn thiện đủ nội dung |

### IELTS - Nghe

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `IELTS Placement Listening` |
| Thời lượng | `20` phút |
| Nội dung | Dùng lại phần `Library Registration` ở mục 7 và có thể bổ sung câu hỏi để tăng độ phân hóa |

### IELTS - Đọc

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `IELTS Placement Reading` |
| Thời lượng | `25` phút |
| Nội dung | Dùng lại phần `Urban Green Spaces` ở mục 8 và bổ sung câu hỏi từ vựng, ý chính |

### IELTS - Viết

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `IELTS Placement Writing` |
| Thời lượng | `40` phút |
| Task | Dùng đề `Free University Education` ở mục 9 |

### IELTS - Nói

| Trường | Giá trị nhập |
|---|---|
| Tiêu đề | `IELTS Placement Speaking` |
| Thời lượng | `14` phút |
| Nội dung | Dùng ba phần `Education and Learning` ở mục 10 |

### TOEIC

| Trường | Giá trị nhập |
|---|---|
| Tên cấu hình | `TOEIC Placement` |
| Listening | `45` phút, Part 1-4 |
| Reading | `75` phút, Part 5-7 |
| Khởi tạo cấu trúc | Bấm `Nạp khung 7 part`, sau đó nhập câu hỏi và đáp án theo từng part |

---

## 13. Khóa học Online

Đường dẫn: `/content-manager/courses`

### Thông tin khóa học

| Trường | Giá trị nhập |
|---|---|
| Tên khóa học | `IELTS Foundation to Band 6.5` |
| Danh mục | `IELTS` |
| Trình độ | `INTERMEDIATE` |
| Thời gian hoàn thành dự kiến | `12 tuần` |
| Band IELTS đầu vào | `4.0` |
| Band IELTS mục tiêu | `6.5` |
| Đầu ra / kết quả | `Hoàn thành khóa học, học viên có thể xử lý các dạng bài IELTS phổ biến, xây dựng câu trả lời có chiến lược và hướng tới Band 6.5 ở cả bốn kỹ năng.` |
| Giá bán | `3990000` |
| Giá ưu đãi | `2990000` |
| Ảnh bìa | Tải lên ảnh tỷ lệ ngang bằng nút tải ảnh |
| Khóa học nổi bật | `Bật` |
| Mô tả ngắn | `Lộ trình IELTS bốn kỹ năng dành cho học viên từ Band 4.0, tập trung chiến lược làm bài và luyện tập có hướng dẫn.` |
| Mô tả đầy đủ | `Khóa học xây dựng nền tảng từ vựng, ngữ pháp và chiến lược cho Listening, Reading, Writing và Speaking. Mỗi mô-đun kết hợp bài giảng, học liệu, bài luyện ngắn và bài đánh giá để học viên theo dõi tiến bộ rõ ràng.` |
| Trạng thái ban đầu | `Bản nháp` |

### Mô-đun 1

| Trường | Giá trị nhập |
|---|---|
| Tên mô-đun | `IELTS Orientation and Core Strategies` |
| Mô tả | `Làm quen cấu trúc bài thi, cách quản lý thời gian và chiến lược xác định từ khóa.` |

### Bài học 1.1

| Trường | Giá trị nhập |
|---|---|
| Tên bài học | `Understanding the IELTS Test Format` |
| Mô tả | `Tổng quan bốn kỹ năng, cách tính điểm và những lỗi chuẩn bị thường gặp.` |
| Loại nội dung | `ARTICLE` |
| Thời lượng | `25` phút |
| Liên kết tài liệu | Chọn học liệu `IELTS Writing Task 2 - Opinion Essay Planner` nếu phù hợp, hoặc để trống |
| Cho phép xem trước | `Bật` |
| Nội dung bài học | `IELTS Academic gồm bốn kỹ năng: Listening, Reading, Writing và Speaking. Điểm tổng là trung bình của bốn kỹ năng và được làm tròn theo quy tắc IELTS. Trước khi luyện đề, học viên cần hiểu thời lượng, số phần và yêu cầu của từng kỹ năng.` |

### Bài học 1.2

| Trường | Giá trị nhập |
|---|---|
| Tên bài học | `Keyword and Paraphrase Strategies` |
| Mô tả | `Nhận diện từ khóa và các cách diễn đạt tương đương trong câu hỏi.` |
| Loại nội dung | `VIDEO` |
| Video | Tải video lên hệ thống hoặc dùng liên kết YouTube/Bunny hợp lệ |
| Cho phép xem trước | `Tắt` |
| Bộ flashcard gắn kèm | `IELTS Writing - Education Vocabulary` |

### Mô-đun 2

| Trường | Giá trị nhập |
|---|---|
| Tên mô-đun | `Writing Task 2 Foundations` |
| Mô tả | `Xây dựng lập trường, dàn ý và đoạn văn cho bài luận Task 2.` |

### Bài học 2.1

| Trường | Giá trị nhập |
|---|---|
| Tên bài học | `Planning an Opinion Essay` |
| Mô tả | `Phân tích đề và lập dàn ý cho dạng Agree or Disagree.` |
| Loại nội dung | `ASSIGNMENT` |
| Bài tập từ ngân hàng | `Lập dàn ý Opinion Essay về giáo dục đại học` |
| Thời lượng | `35` phút |

Sau khi lưu cấu trúc, xem trước toàn bộ khóa học rồi mới tạo phiên bản và xuất bản.

---

## 14. Khóa học có giảng viên

Đường dẫn: `/content-manager/instructor-led-courses`

### Thông tin khóa học

| Trường | Giá trị nhập |
|---|---|
| Tên khóa học | `IELTS Intensive 6.5 - Instructor Led` |
| Mã khóa học | `IELTS-INTENSIVE-65-01` |
| Danh mục / Nhóm thi | `IELTS` |
| Trạng thái | `Bản nháp` |
| Thời gian hoàn thành dự kiến | `24 buổi (48 giờ) - 12 tuần` |
| Điểm đầu vào | `4.5` |
| Band IELTS mục tiêu | `6.5` |
| Kỹ năng trọng tâm | Chọn `Listening`, `Reading`, `Writing`, `Speaking` |
| Đầu ra | `Học viên áp dụng được chiến lược làm bài cho bốn kỹ năng, hoàn thành bài Writing Task 2 có cấu trúc rõ và duy trì bài Speaking Part 2 trong hai phút.` |
| Mô tả ngắn | `Khóa IELTS chuyên sâu có giáo viên hướng dẫn trực tiếp, chữa bài định kỳ và theo dõi tiến độ cá nhân.` |
| Mô tả đầy đủ | `Chương trình kéo dài 12 tuần, kết hợp kiến thức nền, chiến lược làm bài và thực hành trên lớp. Giáo viên sử dụng học liệu dùng chung, giao bài sau mỗi Unit và phản hồi theo bộ tiêu chí IELTS.` |

### Unit 1

| Trường | Giá trị nhập |
|---|---|
| Tên Unit | `Diagnostic and IELTS Orientation` |
| Thứ tự hiển thị | `1` |
| Mô tả Unit | `Đánh giá năng lực ban đầu và thống nhất chiến lược học tập cho khóa học.` |

### Bài học Unit 1

| Trường | Giá trị nhập |
|---|---|
| Thứ tự bài học | `1` |
| Số buổi dự kiến | `2` |
| Tiêu đề bài học | `Diagnostic Test and Goal Setting` |
| Mô tả bài học | `Học viên làm bài chẩn đoán rút gọn và phân tích điểm mạnh, điểm cần cải thiện.` |
| Mục tiêu học tập | `Hiểu cấu trúc IELTS; xác định band hiện tại; lập mục tiêu học tập cho 12 tuần.` |

### Unit 2

| Trường | Giá trị nhập |
|---|---|
| Tên Unit | `Writing Task 2 - Building Arguments` |
| Thứ tự hiển thị | `2` |
| Mô tả Unit | `Phân tích đề, xây dựng luận điểm và phát triển ví dụ cho bài luận Task 2.` |

### Bài học Unit 2

| Trường | Giá trị nhập |
|---|---|
| Thứ tự bài học | `2` |
| Số buổi dự kiến | `3` |
| Tiêu đề bài học | `Opinion Essay Structure and Development` |
| Mô tả bài học | `Thực hành viết thesis statement, topic sentence và đoạn thân bài theo cấu trúc PEEL.` |
| Mục tiêu học tập | `Viết được dàn ý hoàn chỉnh; duy trì lập trường nhất quán; phát triển đoạn văn bằng giải thích và ví dụ.` |

### Nội dung gắn vào Unit 2

| Loại | Chọn dữ liệu |
|---|---|
| Học liệu trung tâm | `IELTS Writing Task 2 - Opinion Essay Planner` |
| Ngân hàng bài tập | `Lập dàn ý Opinion Essay về giáo dục đại học` |
| Bộ Flashcard | `IELTS Writing - Education Vocabulary` |

Chỉ xuất bản sau khi khóa học có ít nhất một Unit, một bài học và chuẩn đầu ra hợp lệ.

---

## 15. Lộ trình học

Đường dẫn: `/content-manager/learning-paths`

| Trường | Giá trị nhập |
|---|---|
| Mã lộ trình | `IELTS-ROADMAP-65-01` |
| Tên lộ trình | `Lộ trình IELTS từ 4.0 đến 6.5` |
| Kỳ thi | `IELTS` |
| Band mục tiêu | `6.5` |
| Giảm giá lộ trình | `10` |
| Số khóa tối thiểu được giảm | `2` |

Sau khi tạo, thêm và sắp xếp ít nhất hai khóa IELTS đã xuất bản. Nếu mới có một khóa mẫu, tạo thêm một khóa nối tiếp trước khi kiểm tra ưu đãi lộ trình.

---

## 16. Mã giảm giá

Đường dẫn: `/content-manager/discount-codes`

| Trường | Giá trị nhập |
|---|---|
| Mã | `IELTSWELCOME20` |
| Tên hiển thị | `Ưu đãi 20% khóa IELTS` |
| Loại giảm giá | `Phần trăm` |
| Giá trị | `20` |
| Giới hạn sử dụng | `100` |
| Bắt đầu từ | `16/09/2026 00:00` |
| Hết hạn lúc | `31/12/2026 23:59` |
| Đang kích hoạt | `Bật` |

---

## 17. Các mục không tạo dữ liệu trực tiếp

### Tổng quan

Đường dẫn: `/content-manager/dashboard`

Không có form tạo. Dữ liệu tự tổng hợp từ khóa học Online và các hoạt động quản lý gần đây.

### Hàng chờ xuất bản

Đường dẫn: `/content-manager/publication`

Không tạo bản ghi riêng. Khóa học hoặc nội dung được gửi duyệt sẽ tự xuất hiện tại đây.

### Phân tích nội dung

Đường dẫn: `/content-manager/analytics`

Không tạo bản ghi riêng. Số liệu hình thành từ nội dung đã xuất bản, lượt học và kết quả làm bài.

### Báo cáo hỏi đáp

Đường dẫn: `/content-manager/discussion-moderation`

Không tạo thủ công bằng Content Manager. Cần tài khoản học viên gửi báo cáo một nội dung thảo luận; báo cáo sau đó mới xuất hiện để ẩn hoặc bỏ qua.

---

## 18. Checklist kiểm tra sau khi nhập

- Danh mục `IELTS` đang hoạt động.
- Hai bộ tiêu chí Writing và Speaking ở trạng thái `Đang dùng`.
- Học liệu mở hoặc tải xuống được.
- Bộ thẻ có đủ bốn thẻ và hiển thị đúng danh mục IELTS.
- Bài nghe và bài đọc chấm đúng các đáp án mẫu.
- Bài viết và bài nói đã liên kết đúng bộ tiêu chí tương ứng.
- Đề thi thử hiển thị đúng `TOEIC` và `Listening`.
- Bài đánh giá đầu vào chỉ bật sau khi đủ nội dung.
- Khóa học Online mở đúng bài được chọn trong từng mô-đun.
- Khóa học có giảng viên có Unit, bài học và tài nguyên gắn kèm.
- Lộ trình chứa đúng thứ tự khóa học và áp dụng ưu đãi khi đủ số khóa tối thiểu.
- Mã giảm giá còn thời hạn, đang kích hoạt và chưa vượt giới hạn sử dụng.
