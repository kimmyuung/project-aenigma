package kmh.project.voucher.storage.employee;

import jakarta.persistence.*;
import kmh.project.voucher.storage.BaseEntity;

@Table(name = "employee")
@Entity
public class EmployeeEntity extends BaseEntity {

    private String name;
    private String postion;
    private String department;

    public EmployeeEntity() {}

    public EmployeeEntity(String name, String postion, String department) {
        this.name = name;
        this.postion = postion;
        this.department = department;
    }


    public String name() {return name;}
    public String department() {return department;}
    public String position() {return postion;}
}
