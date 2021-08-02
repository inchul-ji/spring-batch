package kr.co.starlabs.batch.part3;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@NoArgsConstructor
@Getter
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private String age;

    private String address;

    public Person(String name, String age, String address) {
        this(0,name, age,address);
        // id를 0 으로 설정하면 JPA 에서 알아서 AutoIncrement 를 해주기 때문에 문제 없다.

//        this.name = name;
//        this.age = age;
//        this.address = address;
    }

    public Person(int id, String name, String age, String address) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
    }
}
