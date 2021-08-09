package kr.co.starlabs.batch.part6;

import kr.co.starlabs.batch.part4.LevelUpJobExecutionListener;
import kr.co.starlabs.batch.part4.SaveUserTasklet;
import kr.co.starlabs.batch.part4.User;
import kr.co.starlabs.batch.part4.UserRepository;
import kr.co.starlabs.batch.part5.JobParametersDecide;
import kr.co.starlabs.batch.part5.OrderStatistics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;

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
public class ParallelUserConfiguration {

    private final int CHUNK = 1000;
    private final String JOB_NAME = "parallelUserJob";

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final UserRepository userRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;
    private final TaskExecutor taskExecutor;


    public ParallelUserConfiguration(JobBuilderFactory jobBuilderFactory,
                                     StepBuilderFactory stepBuilderFactory,
                                     UserRepository userRepository,
                                     EntityManagerFactory entityManagerFactory,
                                     DataSource dataSource,
                                     TaskExecutor taskExecutor) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.userRepository = userRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
        this.taskExecutor = taskExecutor;
    }


    @Bean(JOB_NAME)
    public Job userJob() throws Exception {
        return this.jobBuilderFactory.get(JOB_NAME)
                .incrementer(new RunIdIncrementer())
                .listener(new LevelUpJobExecutionListener(userRepository))
                .start(this.saveUserFlow())
                .next(this.splitFlow(null))
//                .next(this.userLevelUpStep())
//                .next(new JobParametersDecide("date")) // date라는 파라미터가 있는지 확인, 있을 때만 아래 내용 실행
//                .on(JobParametersDecide.CONTINUE.getName())
//                .to(this.orderStatisticsStep(null))
                .build()
                .build();
    }

    @Bean(JOB_NAME + "_saveUserFlow")
    public Flow saveUserFlow() {
        TaskletStep saveUserStep = this.stepBuilderFactory.get(JOB_NAME + "_saveUserStep")
                .tasklet(new SaveUserTasklet(userRepository))
                .build();

        return new FlowBuilder<SimpleFlow>(JOB_NAME + "_saveUserFlow")
                .start(saveUserStep)
                .build();
    }

    /**
     * flow 2개를 합친다. (병렬처리 한다.)
     */
    @Bean(JOB_NAME + "_splitFlow")
    @JobScope
    public Flow splitFlow(@Value("#{jobParameters[date]}") String date) throws Exception {
        Flow userLevelUpFlow = new FlowBuilder<SimpleFlow>(JOB_NAME + "_userLevelUpFlow")
                .start(userLevelUpStep())
                .build();

        return new FlowBuilder<SimpleFlow>(JOB_NAME + "_splitFlow")
                .split(this.taskExecutor)
                .add(userLevelUpFlow, orderStatisticsFlow(date))
                .build();

    }

    public Flow orderStatisticsFlow(String date) throws Exception {
        return new FlowBuilder<SimpleFlow>(JOB_NAME + "_orderStatisticsFlow")
                .start(new JobParametersDecide("date")) // date라는 파라미터가 있는지 확인, 있을 때만 아래 내용 실행
                .on(JobParametersDecide.CONTINUE.getName())
                .to(this.orderStatisticsStep(date))
                .build();

    }


    //    @Bean(JOB_NAME + "_orderStatisticsStep")
//    @JobScope
    public Step orderStatisticsStep(@Value("#{jobParameters[date]}") String date) throws Exception {
        return this.stepBuilderFactory.get(JOB_NAME + "_orderStatisticsStep")
                .<OrderStatistics, OrderStatistics>chunk(CHUNK)
                .reader(orderStatisticsItemReader(date))
                .writer(orderStatisticsItemWriter(date))
                .build();


    }

    private ItemWriter<? super OrderStatistics> orderStatisticsItemWriter(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        // csv 파일 이름
        String filename = yearMonth.getYear() + "년" + yearMonth.getMonthValue() + "월_일별 주문_금액.csv";

        BeanWrapperFieldExtractor<OrderStatistics> fieldExtractor = new BeanWrapperFieldExtractor<>();

        fieldExtractor.setNames(new String[]{"amount", "date"});

        DelimitedLineAggregator<OrderStatistics> lineAggregator = new DelimitedLineAggregator<>();
        lineAggregator.setDelimiter(",");
        lineAggregator.setFieldExtractor(fieldExtractor);

        // 정크가 완전히 종료될 떄까지 파일을 생성하지 않고 있다가 모두 마무리 되면 생성한다.
        FlatFileItemWriter<OrderStatistics> itemWriter = new FlatFileItemWriterBuilder<OrderStatistics>()
                .resource(new FileSystemResource("output/" + filename))
                .lineAggregator(lineAggregator)
                .name(JOB_NAME + "_orderStatisticsItemWriter")
                .encoding("UTF-8")
                .headerCallback(writer -> writer.write("total_amount,date"))
                .build();

        itemWriter.afterPropertiesSet();

        return itemWriter;
    }

    private ItemReader<? extends OrderStatistics> orderStatisticsItemReader(String date) throws Exception {
        YearMonth yearMonth = YearMonth.parse(date);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", yearMonth.atDay(1)); // date 가 2020-11 로 들어왔다면 2020-11-01 이 된다.
        parameters.put("endDate", yearMonth.atEndOfMonth()); // 해당 월의 마지막 일자

        Map<String, Order> sortKey = new HashMap<>();
        sortKey.put("create_date", Order.ASCENDING);

        JdbcPagingItemReader<OrderStatistics> itemReader = new JdbcPagingItemReaderBuilder<OrderStatistics>()
                .dataSource(this.dataSource)
                .rowMapper((rs, rowNum) -> OrderStatistics.builder()
                        .amount(rs.getString(1))
                        .date(LocalDate.parse(rs.getString(2), DateTimeFormatter.ISO_DATE))
                        .build())
                .pageSize(CHUNK)
                .name(JOB_NAME + "_orderStatisticsItemReader")
                .selectClause("sum(amount), create_date")
                .fromClause("orders")
                .whereClause("create_date >= :startDate and create_date <= :endDate")
                .groupClause("create_date")
                .parameterValues(parameters)
                .sortKeys(sortKey)
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }

//    @Bean(JOB_NAME + "_saveUserStep")
//    public Step saveUserStep() {
//        return this.stepBuilderFactory.get(JOB_NAME + "_saveUserStep")
//                .tasklet(new SaveUserTasklet(userRepository))
//                .build();
//    }

    @Bean(JOB_NAME + "_userLevelUpStep")
    public Step userLevelUpStep() throws Exception {
        return this.stepBuilderFactory.get(JOB_NAME + "_userLevelUpStep")
                .<User, User>chunk(CHUNK)
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
            if (user.availableLevelUp()) { // 등급 상향 대상의 유저인지 체크
                return user;
            }
            return null;
        };
    }

    private ItemReader<? extends User> itemReader() throws Exception {
        JpaPagingItemReader<User> itemReader = new JpaPagingItemReaderBuilder<User>()
                .queryString("select u from User u") // jpql 쿼리
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK) // 보통 chunk 사이즈와 동일하게 설정한다.
                .name(JOB_NAME + "_userItemReader")
                .build();

        itemReader.afterPropertiesSet();

        return itemReader;
    }


}
