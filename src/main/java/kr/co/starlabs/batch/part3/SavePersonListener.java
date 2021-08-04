package kr.co.starlabs.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.annotation.BeforeStep;

@Slf4j
public class SavePersonListener {

    public static class SavePersonJobExecutionListener implements JobExecutionListener {
        // job 실행 전 호출
        @Override
        public void beforeJob(JobExecution jobExecution) {
            log.info("interface beforeJob");
        }

        // job 실행 후 호출
        @Override
        public void afterJob(JobExecution jobExecution) {
            int sum = jobExecution.getStepExecutions().stream().mapToInt(StepExecution::getWriteCount).sum();
            log.info("interface afterJob sum: [ {} ]", sum);
        }
    }

    // Annotation 기반 방법
    public static class SavePersonAnnotationJobExecutionListener {

        // job 실행 전 호출
        @BeforeJob
        public void beforeJob(JobExecution jobExecution) {
            log.info("annotation beforeJob");
        }

        // job 실행 후 호출
        @AfterJob
        public void afterJob(JobExecution jobExecution) {
            int sum = jobExecution.getStepExecutions().stream().mapToInt(StepExecution::getWriteCount).sum();
            log.info("annotation afterJob sum: [ {} ]", sum);
        }
    }


    public static class SavePersonStepExecutionListener {
        @BeforeStep
        public void beforeStep(StepExecution stepExecution) {
            log.info("beforeStep");
        }

        @AfterStep
        public ExitStatus afterStep(StepExecution stepExecution) {
            log.info("afterStep : [ {} ]", stepExecution.getWriteCount());
//            if (stepExecution.getWriteCount() == 0) {
//                return ExitStatus.FAILED;
//            }
            return stepExecution.getExitStatus();
        }


    }



}
