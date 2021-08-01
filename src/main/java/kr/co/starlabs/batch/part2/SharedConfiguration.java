package kr.co.starlabs.batch.part2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SharedConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public SharedConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job shareJob() {
        return jobBuilderFactory.get("shareJob")
                .incrementer(new RunIdIncrementer())
                .start(this.shareStep()) // 최초로 실행될 step 설정.
                .next(this.shareStep2()) // start() 가 끝나고 다음에 시작할 step 을 지정, 순차적으로 실행된다.
                .build();
    }


    @Bean
    public Step shareStep() {
        return stepBuilderFactory.get("shareStep")
                .tasklet((contribution, chunkContext) -> { // tasklet 을 실행하면 contribution 이라는 객체가 있다.
                    StepExecution stepExecution = contribution.getStepExecution(); // contribution 을 통해 stepExecution 객체를 꺼낼 수 있다.
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();    // stepExecution 에서 stepExecutionContext 를 꺼낼 수 있다.
                    stepExecutionContext.putString("stepKey", "step execution context 에 넣는 데이터");    // stepExecutionContext 에 데이터를 넣는다. (key: value 형태로 저장)

                    JobExecution jobExecution = stepExecution.getJobExecution();    // stepExecution 에서 jobExecution 을 꺼낼 수 있다.
                    JobInstance jobInstance = jobExecution.getJobInstance();    // jobExecution 에서 jobInstance 를 꺼낸다.
                    ExecutionContext jonExecutionContext = jobExecution.getExecutionContext();  // jobExecution 에서 jobExecutionContext 를 꺼낸다.
                    jonExecutionContext.putString("jobKey", "job execution context 에 넣는 데이터");   // jobExecutionContext 에 key:value 형태로 값을 저장한다.
                    
                    JobParameters jobParameters = jobExecution.getJobParameters();

                    log.info(">>> jobName : [ {} ], stepName : [ {} ], parameter : [ {} ]",
                            jobInstance.getJobName(),
                            stepExecution.getStepName(),
                            jobParameters.getLong("run.id") // new RunIdIncrementer() 를 통해 자동으로 생성된 run.id parameter 를 출력한다.
                    );
                    
                    return RepeatStatus.FINISHED;
                })
                .build();
    }


    @Bean
    public Step shareStep2() {
        return stepBuilderFactory.get("shareStep2")
                .tasklet((contribution, chunkContext) -> {
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

                    log.info(">>> stepKey  : [ {} ], jobKey : [ {} ]",
                            stepExecutionContext.getString("stepKey", "emptyStepKey"), // 이전에 stepContext 에 넣어둔 데이터를 꺼내려고 하기 때문에 찾을 수 없다. 두 번째 인자는 값이 없을 때 사용할 default 값을 지정한다.
                            jobExecutionContext.getString("jobKey", "emptyJobKdy")  // 같은 Job 내에서 step 간에 jobExecutionContext의 데이터를 공유할 수 있기 때문에 이전 step 에서 넣어둔 값을 꺼낼 수 있다.
                    );

                    return RepeatStatus.FINISHED;
                })
                .build();
    }




}
