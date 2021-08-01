package kr.co.starlabs.batch.part3;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class ChunkProcessingConfiguration {

    /**
     * <INPUT, OUTPUT>chunk(int)
     *  reader에서 INPUT 을 return
     *
     *  processor에서 INPUT을 받아 processing 후 OUPUT을 return
     *
     *  INPUT, OUTPUT은 같은 타입일 수 있음
     *      writer에서 List<OUTPUT>을 받아 write
     */


    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public ChunkProcessingConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job chunkProcessingJob() {
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new RunIdIncrementer())
                .start(this.taskBaseStep())
                .next(this.chunkBaseStep(null))
                .build();
    }


    /**
     * task 기반
     */
    @Bean
    public Step taskBaseStep() {
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(this.tasklet(null))
                .build();
    }


    @Bean
    @StepScope
    public Tasklet tasklet(@Value("#{jobParameters[chunkSize]}") String value) {
        List<String> items = getItems();

        return (contribution, chunkContext) -> {
//            List<String> items = getItems();
            StepExecution stepExecution = contribution.getStepExecution();
            JobParameters jobParameters = stepExecution.getJobParameters();


            /**
             * JobParameters 방식
             */
//            String value = jobParameters.getString("chunkSize", "10");  // 실행 설정명렁어(Program arguments)에 [ -chunkSize=20 --job.name=chunkProcessingJob ] 와 같은 형태로 chunkSize 를 설정했다.
            int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;
            int fromIndex = stepExecution.getReadCount(); // 0 -> 10 -> 20 -> 30 ....
            int toIndex = fromIndex + chunkSize;

            if (fromIndex >= items.size()) {
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIndex, toIndex);

            log.info("task subList size : [ {} ]", subList.size());

//            log.info("task Item size : [ {} ]", items.size());

            stepExecution.setReadCount(toIndex);

            return RepeatStatus.CONTINUABLE;
        };
    }


    /**
     * Spring EL(Expression Language) 방식
     */
    @Bean
    @JobScope
    public Step chunkBaseStep(@Value("#{jobParameters[chunkSize]}") String chunkSize) {
        return stepBuilderFactory.get("chunkBaseStep")
                // 첫 번째 Generic Type이 ItemReader 에서 읽고, 반환하는 타입 (Input Type)
                // 두 번째 Generic Type 으로 ItemProcessor 에서 처리하고 반환하는 타입 (Output Type)
                .<String, String>chunk(StringUtils.isNotEmpty(chunkSize) ? Integer.parseInt(chunkSize) : 10)  // 큰 데이터를 10개씩 나누라는 의미. (큰 데이터를 10번 나누라는 의미가 아님. 다르다.)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }


    /**
     * ItemReader 는 item 을 1개씩 처리한다.
     * @return
     */
    private ItemReader<String> itemReader() {
        return new ListItemReader<>(getItems());
    }

    /**
     * ItemProcessor 는 item 을 1개씩 처리한다.
     * @return
     */
    private ItemProcessor<String, String> itemProcessor() {
        return item -> item + ", Spring Batch"; // 만약 null 을 return 하면 ItemWriter 로 넘어갈 수 없게 된다.
    }


    /**
     * ItemWriter 는 item 을 묶어서(chunk 에서 저한 숫자만큼) 처리한다.
     * @return
     */
    private ItemWriter<String> itemWriter() {
        return items -> log.info("chunk items size : [ {} ]", items.size()); // 10, 10, 10 ... 10 (총 10개의 log 가 찍힌다.)
//        return items -> items.forEach(log::info);
    }


    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(i + " Hello");
        }

        return items;
    }
}
