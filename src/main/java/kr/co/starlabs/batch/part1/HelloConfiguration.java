package kr.co.starlabs.batch.part1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HelloConfiguration {

    /**
     * job : batch의 실행 단위
     * JobBuilderFactory : job 을 만들 수 있는 클래스
     *  - Spring batch에 의해 이미 Bean으로 생성되어 있어, 생성자 주입으로 받을 수 있다.
     *
     *  RunIdIncrementer : 항상 Job 이 실행 할 때마다 파라미터 id를 자동으로 생성해주는 Class
     */
    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;

    public HelloConfiguration(JobBuilderFactory jobBuilderFactory,
                              StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    /**
     * helloJob : Job 이름
     *  - 스프링 배치를 실행할 수 있는 Key 역할도 한다.
     *
     * @return
     */
    @Bean
    public Job helloJob() {
        return jobBuilderFactory.get("helloJob")
                .incrementer(new RunIdIncrementer())
                .start(this.helloStep()) // job 실행 시 최초로 실행될 step을 설정하는 Method
                .build();
    }

    /**
     * Step : Job 의 실행 단위
     *  - 1개의 Job 은 1개 이상의 Step 을 가질 수 있다.
     *
     * helloStep : step 이름
     *
     *
     * Job, Step은 Spring batch 에서 거의 모든 것이라고 볼 수 있다.
     * @return
     */
    @Bean
    public Step helloStep() {
        return stepBuilderFactory.get("helloStep")
                .tasklet((contribution, chunkContext) -> { // tasklet 에 실행 단위 설정. tasklet 의 반대는 chunk
                    log.info("hello spring batch!!");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    /**
     * 그냥 실행하면 모든 Job 이 실행된다.
     * 그렇기에 실행 명령어에 옵션을 주던가, yml 파일과 같은 설정 파일에 설정값을 넣어준다.
     *
     *  # 설정 명렁어 (program argument)
     *      --spring.batch.job.names=helloJob   : Application 이 실행될 때 helloJob 이라는 job 만 실행하겠다는 의미
     *
     *
     *  # yml 설정
     *  - spring.vatch.job.names 라는 설정 값을 job.name 으로 커스터마이징,
     *  - 이렇게 작성해주면 application 실행 시 모든 Job 을 실행하지 않아 Application 실행하면서 자동으로 실행되는 것을 막을 수 있다.
     *
     *  spring:
     *      batch:
     *      job:
     *          names: ${job.name:NONE}
     *
     *
     *  이후, 시작 명령어 사용법
     *  --job.name=helloJob
     *
     *
     *  [ 정리 ]
     *  Job : Spring batch 실행 단위
     *  Step : Job 의 실행 단위
     *
    */

}
