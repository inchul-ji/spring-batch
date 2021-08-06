package kr.co.starlabs.batch.part3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

@Slf4j
public class PersonValidationRetryProcessor implements ItemProcessor<Person, Person> {

    private final RetryTemplate retryTemplate;

    public PersonValidationRetryProcessor() {
        this.retryTemplate = new RetryTemplateBuilder()
                .maxAttempts(3)
                .retryOn(NotFoundNameException.class) // NotFoundNameException이 3번 발생할 때 까지는 허용하고, 재시도한다.
                .withListener(new SavePersonRetryListener())
                .build();
    }

    @Override
    public Person process(Person item) throws Exception {
        return this.retryTemplate.execute(context -> {
            // RetryCallback : RetryTemplate의 첫 시작점 (즉 process가 시작될 때 처음 시작되는 시작점)
            // 위의 constructor 에 있는 maxAttempts() 의 설정 수 만큼 retryCallback 이 실행된다.

            if (item.isNotEmptyName()) {
                return item;
            }

            throw new NotFoundNameException();

        }, context -> {
            // RecoveryCallback : 현재 설정 상 RetryCallback에서  NotFoundNameException이 3번 발생하면 RecoveryCallback 이 호출된다.

            return item.unKnownName();


        });
    }

    public static class SavePersonRetryListener implements RetryListener {

        // retry를 시작하는 설정
        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            return true; // true: retry 적용
        }

        // retry 종료 후 호출
        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.info("SavePersonRetryListener close()");
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            log.info("SavePersonRetryListener onError()");
        }
    }


}
