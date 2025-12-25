package kmh.project.voucher.app.controller.employee.response;

import java.time.LocalDateTime;

public record EmployeeResponse(Long no, String name, String position, String department
, LocalDateTime crateAt, LocalDateTime updateAt
                               ) {
}
