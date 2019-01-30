package hello.restTemplate;

import hello.entity.Employee;
import hello.repository.EmployeeRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static com.jcabi.matchers.RegexMatchers.matchesPattern;
import static hello.controller.Route.EMPLOYEE_ROUTE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EmployeeRestTemplateTest {

    private String employeeUrl;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Before
    public void setUp() {
        String url = "http://localhost:" + port;
        employeeUrl = url + EMPLOYEE_ROUTE;
    }

    @After
    public void cleanUp() {
        // manually clear db
        employeeRepository.deleteAll();
    }

    @Test
    public void getEmployeeById() {
        Employee employeeExpected = new Employee("Foo", "foo@foo.com");

        restTemplate.postForEntity(employeeUrl, employeeExpected, Employee.class);

        ResponseEntity<Employee[]> responseObject = restTemplate.getForEntity(employeeUrl, Employee[].class);
        Employee[] employees = responseObject.getBody();
        Employee createdEmployee = employees[0];
        Long id = createdEmployee.getId();
        employeeExpected.setId(id);

        ResponseEntity<Employee> responseEntity = restTemplate.getForEntity(employeeUrl + "/" + id, Employee.class);
        Employee employee = responseEntity.getBody();

        assertEquals(employeeExpected, employee);
    }

    @Test
    public void employeeList_WhenEmpty() {
        ResponseEntity<Employee[]> responseObject = restTemplate.getForEntity(employeeUrl, Employee[].class);
        Object[] employees = responseObject.getBody();
        List<?> searchList = Arrays.asList(employees);
        assertEquals(0, searchList.size());
    }

    @Test
    public void employeeList_WhenInserted() {
        // insert
        Employee employee = new Employee("Foo", "foo@foo.com");
        restTemplate.postForEntity(employeeUrl, employee, Employee.class);

        ResponseEntity<Employee[]> responseObject = restTemplate.getForEntity(employeeUrl, Employee[].class);
        Employee[] employees = responseObject.getBody();
        Employee firstEmployee = employees[0];

        assertThat(firstEmployee.getName(), equalTo(employee.getName()));
        assertThat(firstEmployee.getRole(), equalTo(employee.getRole()));
    }

    @Test
    public void createEmployee_WhenFirst() {
        ResponseEntity<Employee> responseEntity =
                restTemplate.postForEntity(employeeUrl, new Employee("Foo", "foo@foo.com"), Employee.class);
        HttpHeaders headers = responseEntity.getHeaders();
        List<String> location = headers.get("Location");

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String actual = location.get(0);
        assertThat(actual, matchesPattern(employeeUrl + "/\\d+"));
    }

    @Test
    public void createEmployee_WhenSecond() {
        // First
        restTemplate.postForEntity(employeeUrl, new Employee("Foo", "foo@foo.com"), Employee.class);

        ResponseEntity<Employee> responseEntity =
                restTemplate.postForEntity(employeeUrl, new Employee("Foo2", "foo2@foo.com"), Employee.class);
        HttpHeaders headers = responseEntity.getHeaders();
        List<String> location = headers.get("Location");

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        String actual = location.get(0);
        assertThat(actual, matchesPattern(employeeUrl + "/\\d+"));
    }
}