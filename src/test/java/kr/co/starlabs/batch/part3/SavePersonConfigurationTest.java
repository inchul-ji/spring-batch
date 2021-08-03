package kr.co.starlabs.batch.part3;

import kr.co.starlabs.batch.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class) // spring 테스트가 가능하도록
@ContextConfiguration(classes = {SavePersonConfiguration.class, TestConfiguration.class}) // 테스트 대상, 테스트 config class
class SavePersonConfigurationTest {


    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;


    @Test
    public void test_allow_duplicate() {
        // given
//        new JobParametersBuilder()
//                .addString()

        // when

        // then


    }











}