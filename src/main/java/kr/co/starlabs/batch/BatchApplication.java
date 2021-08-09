package kr.co.starlabs.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** jar 생성
 *  ./gradlew clean jar build -x test  :proejct/build/libs/~ 에 jar 파일이 생긴다.
 *
 *  실행 명령
 *  1) java -jar batch-0.0.1-SNAPSHOT.jar  : 파라미터 없이 실행
 *  2) java -jar batch-0.0.1-SNAPSHOT.jar --job.name=userJob -date=2020-11 -path=C:/project/output/  : 파라미터를 넣으면서 실행 ( --job.name : job 이름 /  -date, -path : 파라미터 이름)
*/
@SpringBootApplication
@EnableBatchProcessing	// batch processing을 하겠다는 의미
public class BatchApplication {

	public static void main(String[] args) {
		System.exit(
				SpringApplication.exit( // 여기까지 넣어주면 Spring batch가 시작하고 종료될 때 안전하게 종료될 수 있도록하는 기능
						SpringApplication.run(BatchApplication.class, args)
				)
		);
	}

	@Bean
	@Primary // spring boot에서 기본적으로 TaskExecutor Bean을 제공해서 내가 만든 Bean을 사용하겠다는 의미
	TaskExecutor taskExecutor() {
		// ThreadPoolTaskExecutor : pool안에서 thread를 미리 생성해놓고 필요할 때 꺼내 쓸 수 있기 떄문에 다른 구현체보다 조금 효율적이다.
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(10); // pool의 기본 thread 크기
		taskExecutor.setMaxPoolSize(20); // 최대 thread 개수 표시
		taskExecutor.setThreadNamePrefix("batch-thread-"); // pool에서 생성된 thread를 사용할 때 log 가 찍히는데 로그의 앞에 찍힐 이름 정의
		taskExecutor.initialize();
		return taskExecutor;
	}

}
