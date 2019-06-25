package test.ericma;

public class Employee {
    private Integer salary;

    private String department;

    public Employee(int salary, String department){
        this.salary = salary;
        this.department = department;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
