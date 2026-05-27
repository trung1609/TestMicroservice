# Review Test Microservice

## Sơ đồ Kiến trúc Hệ thống

```mermaid
flowchart TB
    Client(["Client / Postman"]): client

    ["Cơ sở hạ tầng (Infrastructure)"]
        Gateway["API Gateway (Port: 8686)"]
        Eureka(("Eureka Server (Port: 8761)"))

    Services ["Microservices"]
        Order["Order Service (Port: 8081)"]: service
        Inventory["Inventory Service (Port: 8082)"]: service

    DataLayer ["Lưu trữ & Messaging"]
        OrderDB[("Order DB (PostgreSQL)")]: db
        Kafka{"Apache Kafka (Port: 9092)"}: broker
        InventoryDB[("Inventory DB (PostgreSQL)")]: db

    Luồng API từ ngoài vào
    Client -- "POST /api/v1/orders GET api/v1/products" --> Gateway
    Gateway -- "lb://ORDER-SERVICE" --> Order
    Gateway -- "lb://INVENTORY-SERVICE" --> Inventory

    Luồng Eureka
    Gateway -- "Đăng ký/Khám phá" -- Eureka
    Order -- "Đăng ký" -- Eureka
    Inventory -- "Đăng ký" -- Eureka

    Luồng Nghiệp vụ
    Order -- "1. Check Stock (Feign)" --> Inventory
    Order -- "2. Lưu Đơn hàng" --> OrderDB
    Order -- "3. Pub: order-place-topic" --> Kafka
    Kafka -- "4. Sub: order-place-topic" --> Inventory
    Inventory -- "5. Trừ & Cập nhật Stock" --> InventoryDB
```

---

## Task 3: Lý thuyết

### Câu 1: Giải thích cơ chế "Service Discovery": Tại sao Gateway không nên gọi trực tiếp địa chỉ IP/Port của Service?
**Trả lời:**
1. **Địa chỉ IP động:** Địa chỉ IP có thể thay đổi khi khởi động lại hoặc đổi môi trường. Nếu Gateway hardcode IP, hệ thống sẽ lỗi ngay lập tức khi service đổi IP.
2. **Cân bằng tải (Load Balancing):** Khi có nhiều instance của một service, việc gọi trực tiếp IP sẽ không tận dụng được khả năng load balancing. Service Discovery đóng vai trò cung cấp danh sách instance để Gateway có thể phân phối tải đều.
3. **Khả năng tự phục hồi (Failover):** Nếu một instance bị lỗi, Gateway sẽ không thể tự động chuyển sang instance khác nếu gọi trực tiếp IP. Service Discovery giúp nhận biết và chỉ chuyển hướng lưu lượng đến các instance đang hoạt động.

---

### Câu 2: Nếu request tăng đột biến, làm gì để mở rộng (Scale) Order Service mà không thay đổi cấu hình Gateway?
**Trả lời:**
- **Phương án mở rộng (Scale-out):** Chỉ cần chạy thêm nhiều instance của `Order Service` trên các Port hoặc các Server máy chủ khác nhau.
- **Lý do không cần đổi cấu hình Gateway:** 
  - Các instance `Order Service` mới khi khởi động sẽ tự động đăng ký với **Eureka Server**.
  - Tại Gateway, chúng ta đã cấu hình điều hướng dựa trên Service ID (`lb://ORDER-SERVICE`).
  - Spring Cloud Gateway tích hợp sẵn LoadBalancer sẽ tự động lấy danh sách cập nhật từ Eureka, sau đó cân bằng tải (Round-Robin) vào cả các instance mới và cũ. Do đó, hoàn toàn không cần chạm vào source hay config của Gateway.
- **Tối ưu phụ trợ:** Có thể kết hợp sử dụng Kafka để xử lý bất đồng bộ những luồng nặng, hoặc cấu hình Rate Limiting (giới hạn tỷ lệ request) để giữ hệ thống Order ổn định trong lúc cấu hình mở rộng thêm instance.

---

### Câu 3: So sánh ưu/nhược điểm (Đồng bộ Open Feign vs Bất đồng bộ Kafka) trong bài toán này?
**Trả lời:**

#### 1. Open Feign (Đồng bộ)
- **Ưu điểm:**
  - Dễ triển khai, cho phép gọi qua service khác nhanh chóng bằng interface.
  - **Đảm bảo tính nhất quán dữ liệu:** Khi khách hàng gọi API tạo đơn hàng, hệ thống check đủ tồn kho và báo ngay kết quả thành công/thất bại cho khách hàng.
- **Nhược điểm:**
  - Có thể gây ra độ trễ nếu `Inventory Service` phản hồi chậm, làm giảm trải nghiệm người dùng.
  - **Bị ràng buộc chặt chẽ (Tight coupling):** Nếu `Inventory Service` bị chết hoặc bảo trì, khách hàng hoàn toàn không thể đặt được hàng, gây đứt gãy hệ thống.

#### 2. Kafka (Bất đồng bộ)
- **Ưu điểm:**
  - **Tách rời dịch vụ, chịu lỗi cao:** Khách hàng bấm *Đặt hàng* -> `Order Service` ghi nhận luôn trạng thái `PENDING` và trả về thông báo siêu nhanh. 
  - Kể cả lúc đó `Inventory Service` bị sập, tin nhắn trừ kho vẫn được lưu an toàn trong Kafka chờ đến khi Inventory hoạt động lại để xử lý.
- **Nhược điểm:**
  - Đòi hỏi kiến thức cấu hình hệ thống message queue phức tạp hơn.
  - **Tính nhất quán cuối (Eventual Consistency):** Trong khoảng thời gian trễ của event, hệ thống có thể lỡ tạo 2-3 đơn hàng cùng mua chung 1 sản phẩm. Đến khi Inventory đọc được Event thì hàng trong kho thực ra đã hết, dẫn tới phải code thêm kịch bản *rollback* (hủy đơn, hoàn tiền) rất phức tạp.

