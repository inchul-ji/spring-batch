package kr.co.starlabs.batch.part4;

import kr.co.starlabs.batch.part5.Orders;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveUserTasklet implements Tasklet {

    private final int SIZE = 10_000;
    private final UserRepository userRepository;

    public SaveUserTasklet(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        List<User> users = createUsers();
        Collections.shuffle(users);

        userRepository.saveAll(users);


        return RepeatStatus.FINISHED; // tasklet을 끝낸다는 의미
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>();

        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(1_000)
                            .createDate(LocalDate.of(2020, 11, 1))
                            .itemName("item" + i)
                            .build()))
//                    .totalAmount(1_000)
                    .username("test username" + i)
                    .build());
        }

        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(200_000)
                            .createDate(LocalDate.of(2020, 11, 2))
                            .itemName("item" + i)
                            .build()))
//                    .totalAmount(200_000)
                    .username("test_username" + i)
                    .build());
        }

        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(300_000)
                            .createDate(LocalDate.of(2020, 11, 3))
                            .itemName("item" + i)
                            .build()))
//                    .totalAmount(300_000)
                    .username("test_username" + i)
                    .build());
        }

        for (int i = 0; i < SIZE; i++) {
            users.add(User.builder()
                    .orders(Collections.singletonList(Orders.builder()
                            .amount(500_000)
                            .createDate(LocalDate.of(2020, 11, 4))
                            .itemName("item" + i)
                            .build()))
//                    .totalAmount(500_000)
                    .username("test_username" + i)
                    .build());
        }

        return users;
    }
}
