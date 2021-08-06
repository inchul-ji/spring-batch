package kr.co.starlabs.batch.part4;

import kr.co.starlabs.batch.part5.OrderStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@Slf4j
public class UserConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;


    public UserConfiguration(JobBuilderFactory jobBuilderFactory,
                             StepBuilderFactory stepBuilderFactory,
                             UserRepository userRepository,
                             EntityManagerFactory entityManagerFactory,
                             DataSource dataSource) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.userRepository = userRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }


    @Bean
    public Job userJob() throws Exception {
        return this.jobBuilderFactory.get("userJob")
                .incrementer(new RunIdIncrementer())
                .start(this.saveUserStep())
                .next(this.userLevelUpStep())
                .listener(new LevelUpJobExecutionListener(userRepository))
                .build();
    }

    @Bean
    @JobScope
    public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) {
        return this.stepBuilderFactory.get("orderStatisticsStep")
                .<OrderStatistics, OrderStatistics>chunk(100)
                .reader(orderStatisticsItemReader(date))
                .writer(orderStatisticsItemWriter(date))
                .build();


    }

    private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) {
        YearMonth yearMonth = YearMonth.parse(date);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", yearMonth.atDay(1)); // date 가 2020-11 로 들어왔다면 2020-11-01 이 된다.
        parameters.put("endDate", yearMonth.atEndOfMonth()); // 해당 월의 마지막 일자

        Map<String, Order> sortKey = new HashMap<>();
        sortKey.put("created_date", Order.ASCENDING);

        new JdbcPagingItemReaderBuilder<OrderStatistics>()
                .dataSource(this.dataSource)
                .rowMapper((rs, rowNum) -> OrderStatistics.builder()
                        .amount(rs.getString(1))
                        .date(LocalDate.parse(rs.getString(2), DateTimeFormatter))
                        .build());
        return null;
    }

    @Bean
    public Step saveUserStep() {
        return this.stepBuilderFactory.get("saveUserStep")
                .tasklet(new SaveUserTasklet(userRepository))
                .build();
    }

    @Bean
    public Step userLevelUpStep() throws Exception {
        return this.stepBuilderFactory.get("userLevelUpStep")
                .<User, User>chunk(100)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<? super User> itemWriter() {
        return users -> users.forEach(x -> {
                x.levelUp();
                userRepository.save(x);
            });
    }

    private Function<? super User, ? extends User> itemProcessor() {
        return user -> {
            if (user.availableLevelUp()) { // 등급 상향 유저인지 체크
                return user;
            }
            return null;
        };
    }

    private ItemReader<? extends User> itemReader() throws Exception {
        JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
                .queryString("select u from User u")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(100) // 보통 chunk 사이즈와 동일하게 설정한다.
                .name("userItemReader")
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }


}
