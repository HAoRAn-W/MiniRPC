package one.whr.utils;

/**
 * This is a helper class for holding a value
 */
public class Holder<T> {

    // To ensure that updates to variables propagate predictably to other threads,
    // we should apply the volatile modifier to those variables
    // This way,we communicate with runtime and processor to not reorder any instruction involving the volatile variable.
    // Also, processors understand that they should flush any updates to these variables right away.
    // It's useful in the places where we're ok with multiple threads executing a block of code in parallel,
    // but we need to ensure the visibility property
    private volatile T value;

    public void set(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }
}
