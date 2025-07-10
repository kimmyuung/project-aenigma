package kmh.project.voucher.app.controller;

import kmh.project.voucher.app.controller.request.EmployeeRequest;
import kmh.project.voucher.app.controller.response.EmployeeResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class EmployeeController {
    private final Map<Long, EmployeeResponse> employeeResponseMap = new HashMap<>();

    // 회원 생성
    @PostMapping("/api/v1/employee")
    public Long create(@RequestBody final EmployeeRequest request) {
        Long no = employeeResponseMap.size() + 1L;
        employeeResponseMap.put(no, new EmployeeResponse(no, request.name(), request.position(), request.department()));

        return no;
    }

    // 화원 조회
    @GetMapping("/api/v1/employee/{no}")
    public EmployeeResponse get(@PathVariable final Long no) {
        return employeeResponseMap.get(no);
    }
}
