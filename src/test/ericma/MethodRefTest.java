package test.ericma;

import java.util.Arrays;
import java.util.List;

@FunctionalInterface
interface Supplier<T>{
    T get();
    default public void print(String str){
        System.out.println(str);
    }
}

public class MethodRefTest {

    public static void main(String[] args) {
        final Supplier<Car> supplier = Car::new;
        Car test = supplier.get();
        test.repair();
        Car car = Car.create(Car::new);
        List<Car> carList = Arrays.asList(test,car);
        carList.forEach(System.out::println);
        carList.forEach(Car::collide);
        carList.forEach(test::follow);
        carList.forEach(Car::repair);
    }

}

class Car{
    public static Car create(final Supplier<Car> supplier){
        return supplier.get();
    }

    public static void collide(final Car car){
        System.out.println("Collided" + car.toString());
    }

    public void follow(final Car another){
        System.out.println("Following the " + another.toString());
    }

    public void repair(){
        System.out.println("Repaired " + this.toString());
    }
}
