# Model
## MSAEZ-labcqrs250613
www.msaez.io/#/courses/cna-full/2c7ffd60-3a9c-11f0-833f-b38345d437ae/dp-cqrs-2022
이 작업은 CQRS(Command Query Responsibility Segregation) 패턴을 적용하여 읽기(Read) 전용 모델을 구현한 것입니다.  
쓰기(Write) 모델과 읽기 모델을 분리하고, 이벤트 기반으로 읽기 모델을 업데이트함으로써 서비스 안정성과 데이터 일관성을 확보했습니다.  

![스크린샷 2025-06-13 151103](https://github.com/user-attachments/assets/a74d75d2-dc19-4da1-ae39-d3f8142b2aa4)
![스크린샷 2025-06-13 151838](https://github.com/user-attachments/assets/0c2cbca2-b942-46b0-a2a0-f036e9990b94)
![스크린샷 2025-06-13 152038](https://github.com/user-attachments/assets/86ee0c01-09af-4153-b2be-7ecd75ae7b76)
![스크린샷 2025-06-13 155925](https://github.com/user-attachments/assets/eb2aaf82-eccf-4869-a96c-776ca58a09a5)
![스크린샷 2025-06-13 160034](https://github.com/user-attachments/assets/07ad2da5-bfc8-43db-bd0f-db041e286749)
![스크린샷 2025-06-13 161441](https://github.com/user-attachments/assets/c71a7d4e-b569-4adb-852c-5cc6024a79c0)

---
## 터미널 작성 참고용

1. Java SDK 설치
프로젝트 구동에 필요한 Java Development Kit (JDK)를 설치합니다.  
```
sdk install java
```
2. Kafka 실행
메시지 브로커 역할을 하는 Kafka와 Zookeeper를 Docker Compose를 이용해 실행합니다. docker-compose.yml 파일의 Kafka 버전을 7.5.3으로 수정해야 합니다.  
```
cd infra/
docker-compose up -d
```
3. MyPage 및 Delivery 서비스 코드 수정
CQRS 패턴의 Read 모델을 구현하기 위해 MypageRepository에 findByOrderId 메소드를 추가하고, Delivery.java에 주문 정보가 발생했을 때 Delivery 엔티티를 저장하는 로직을 추가합니다.  

```
Java
// MypageRepository.java 예시
Optional<MyPage> findByOrderId(Long orderId);

// Delivery.java 예시
// ... (OrderPlaced 이벤트 핸들러 내부)
Delivery delivery = new Delivery();
delivery.setAddress(orderPlaced.getAddress());
delivery.setQuantity(orderPlaced.getQty());
delivery.setCustomerId(orderPlaced.getCustomerId());
delivery.setOrderId(orderPlaced.getId());
repository().save(delivery);
// ...
```
4. Order, Delivery, CustomerCenter 서비스 실행
백엔드의 Order (8082 포트), Delivery (8084 포트), CustomerCenter (8085 포트) 마이크로서비스를 각각 실행합니다.  
```
cd order
mvn clean spring-boot:run

# 새 터미널/세션
cd delivery
mvn clean spring-boot:run

# 새 터미널/세션
cd customercenter
mvn clean spring-boot:run
```
5. CQRS 동작 확인
상품 주문을 생성하고, 이후 MyPage 서비스에서 해당 주문의 상태를 조회하여 CQRS 패턴이 올바르게 동작하는지 확인합니다.  
```
http :8082/orders productId=1 qty=1
http :8085/myPages
```
설명: 첫 번째 명령은 상품 주문을 성공적으로 생성하고(HTTP 201 Created), 두 번째 명령은 마이페이지에서 해당 주문을 성공적으로 조회했습니다(HTTP 200 OK).  

6. 서비스 안정성 테스트 (Delivery 서비스 중단 후 MyPage 확인)
배송 서비스(8084 포트)를 중단한 상태에서 MyPage(8085 포트)의 내용을 다시 확인하여, CQRS 패턴에서 Read 모델이 Write 모델의 즉각적인 영향을 받지 않고 안정적으로 정보를 제공하는지 검증합니다.  
```
# delivery 서비스 실행 터미널에서 Ctrl+C 를 눌러 종료
fuser -k 8084/tcp # 또는 이 명령어로 해당 포트의 프로세스 강제 종료
```
```
http :8085/myPages
```
설명: 배송 서비스(8084)를 중단했음에도 마이페이지(8085) 조회는 여전히 성공하고 있습니다. 이는 CQRS 패턴에서 Read 모델(마이페이지)이 Write 모델(주문, 배송)의 장애로부터 독립적일 수 있음을 시사합니다.  

7. Order 서비스 이벤트 발행 로직 수정
Order.java 파일에 주문 생성 (@PostPersist) 및 취소 (@PreRemove) 시 관련 이벤트를 발행하는 로직을 추가하여, 이벤트 기반으로 Read 모델이 업데이트될 수 있도록 합니다.  
```
Java
// Order.java 예시
// ...
@PostPersist
public void onPostPersist() {
    OrderPlaced orderPlaced = new OrderPlaced(this);
    orderPlaced.publishAfterCommit();
}

@PreRemove
public void onPreRemove() {
    OrderCancelled orderCancelled = new OrderCancelled(this);
    orderCancelled.publishAfterCommit();
}
// ...
```
