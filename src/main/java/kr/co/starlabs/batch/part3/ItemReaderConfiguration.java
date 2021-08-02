package kr.co.starlabs.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class ItemReaderConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final EntityManagerFactory entityManagerFactory;

    public ItemReaderConfiguration(JobBuilderFactory jobBuilderFactory,
                                   StepBuilderFactory stepBuilderFactory,
                                   DataSource dataSource,
                                   EntityManagerFactory entityManagerFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
        this.entityManagerFactory = entityManagerFactory;
    }


    @Bean
    public Job itemReaderJob() throws Exception {
        return this.jobBuilderFactory.get("itemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(this.customItemReaderStep())
                .next(this.csvFileStep())
//                .next(this.jdbcStep()) // 실행 순서 : spring boot start -> person.sql 실행 -> 앞의 step 실행 후, jdbcStep 이 실행된다.
                .next(this.jpaStep()) // 실행 순서 : spring boot start -> person.sql 실행 -> 앞의 step 실행 후, jpaStep 이 실행된다.
                .build();
    }

    @Bean
    public Step customItemReaderStep() {
        return this.stepBuilderFactory.get("customItemReaderStep")
                .<Person, Person>chunk(10)
                .reader(new CustomItemReader<>(getItems()))
//                .processor()
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<Person> itemWriter() {
        return items -> log.info("items : [ {} ]", items.stream().map(Person::getName).collect(Collectors.joining(", ")));
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new Person(i + 1, "test name" + i, "test age", "test address"));
        }
        return items;
    }

    @Bean
    public Step csvFileStep() throws Exception {
        return stepBuilderFactory.get("csvFileStep")
                .<Person, Person>chunk(10)
                .reader(this.csvFileItemReader())
                .writer(itemWriter())
                .build();
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        // 데이터를 읽을 수 있는 설정

        // 한줄씩 읽는다.
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>();

        //csv 파일을 Person 객체에 매핑
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "name", "age", "address"); // 필드명 정의 (실제는 id,이름,나이,거주지 이런 순서로 되어 있다.)

        // tokenizer 를 lineMapper 에 주입
        lineMapper.setLineTokenizer(tokenizer);

        // csv파일을 읽겠다.
        lineMapper.setFieldSetMapper(fieldSet -> {
            // 한줄 씩 읽은 데이터를 변수에 넣겠다. (csv 파일 객체에서 읽은 데이터를 뽑아서)
            int id = fieldSet.readInt("id");
            String name = fieldSet.readString("name");
            String age = fieldSet.readString("age");
            String address = fieldSet.readString("address");

            // Person에 매핑
            return new Person(id, name, age, address);
        });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader") // itemReader 이름
                .encoding("UTF-8") // 파일을 읽을 때 인코딩 타입 지정
                .resource(new ClassPathResource("test.csv")) // ClassPathResource : resource 디렉토리를 읽을 수 있는 Class
                .linesToSkip(1) // csv 파일을 보면 첫번 째 row 는 value 가 아니라 필드명이라 읽을 필요가 없다는 의미.
                .lineMapper(lineMapper)
                .build();

        itemReader.afterPropertiesSet(); // afterPropertiesSet : 필수 설정 값이 제대로 설정됐는지 검증하는 메서드

        return itemReader;

    }

    @Bean
    public Step jdbcStep() throws Exception {
        return stepBuilderFactory.get("jdbcStep")
                .<Person, Person>chunk(10)
                .reader(jdbcCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    private JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader")
                .dataSource(dataSource)
                .sql("select id, name, age, address from person")
                .rowMapper((rs, rowNum) -> new Person(
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4)
                ))
                .build();

        itemReader.afterPropertiesSet();
        return itemReader;
    }


    @Bean
    public Step jpaStep() throws Exception {
        return stepBuilderFactory.get("jpaStep")
                .<Person, Person>chunk(10)
                .reader(this.jpaCursorItemReader())
                .writer(itemWriter())
                .build();
    }

    private JpaCursorItemReader<Person> jpaCursorItemReader() throws Exception {
        JpaCursorItemReader<Person> itemReader = new JpaCursorItemReaderBuilder<Person>()
                .name("jpaCursorItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("select p from Person p")  // jpql query
                .build();
        itemReader.afterPropertiesSet();

        return itemReader;
    }


}
