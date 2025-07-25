package kmh.project.voucher.domain.service.employee;

import kmh.project.voucher.app.controller.employee.response.EmployeeResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EmployeeServiceTest {
    @Autowired
    private EmployeeService employeeService;

    @DisplayName("User Create and View the User")
    @Test
    public void Create_ViewEmployee() {
        // given
        String name = "KMH";
        String position = "Commander";
        String department = "Project Manger";

        // when
        Long no = employeeService.create(name, position, department);
        EmployeeResponse response = employeeService.get(no);

        // then
        assertThat(response).isNotNull();
        assertThat(response.no()).isEqualTo(no);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.position()).isEqualTo(position);
        assertThat(response.department()).isEqualTo(department);
    }
}
