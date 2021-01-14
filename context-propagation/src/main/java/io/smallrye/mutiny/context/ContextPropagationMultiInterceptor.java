package io.smallrye.mutiny.context;

import java.util.Objects;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.StrictMultiSubscriber;
import io.smallrye.mutiny.infrastructure.MultiInterceptor;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * Provides context propagation to Multi types.
 * Subclasses need to override this to provide the Context Propagation ThreadContext.
 */
public abstract class ContextPropagationMultiInterceptor implements MultiInterceptor {

    @Override
    public <T> Subscriber<? super T> onSubscription(Publisher<? extends T> instance, Subscriber<? super T> subscriber) {
        return new ContextPropagationSubscriber<>(getThreadContext(), subscriber);
    }

    @Override
    public <T> Multi<T> onMultiCreation(Multi<T> multi) {
        return new ContextPropagationMulti<>(getThreadContext(), multi);
    }

    /**
     * Gets the Context Propagation ThreadContext. External
     * implementations may implement this method.
     *
     * @return the ThreadContext
     * @see DefaultContextPropagationMultiInterceptor#getThreadContext()
     */
    protected abstract SmallRyeThreadContext getThreadContext();

    private static class ContextPropagationMulti<T> extends AbstractMulti<T> {

        private final Multi<T> multi;
        private final SmallRyeThreadContext threadContext;
        private final Object[] context;

        public ContextPropagationMulti(SmallRyeThreadContext threadContext, Multi<T> multi) {
            this.threadContext = threadContext;
            this.context = threadContext.captureContext();
            this.multi = multi;
        }

        @Override
        public void subscribe(Subscriber<? super T> subscriber) {
            Objects.requireNonNull(subscriber); // Required by reactive streams TCK
            Object[] moved = threadContext.applyContext(context);
            try {
                if (subscriber instanceof MultiSubscriber) {
                    multi.subscribe(subscriber);
                } else {
                    multi.subscribe(new StrictMultiSubscriber<>(subscriber));
                }
            } finally {
                threadContext.restoreContext(context, moved);
            }
        }
    }

    @SuppressWarnings({ "ReactiveStreamsSubscriberImplementation" })
    public static class ContextPropagationSubscriber<T> implements Subscriber<T> {

        private final Subscriber<? super T> subscriber;
        private final SmallRyeThreadContext threadContext;
        private final Object[] context;

        public ContextPropagationSubscriber(SmallRyeThreadContext threadContext, Subscriber<? super T> subscriber) {
            this.threadContext = threadContext;
            this.context = threadContext.captureContext();
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            Object[] moved = threadContext.applyContext(context);
            try {
                subscriber.onSubscribe(subscription);
            } finally {
                threadContext.restoreContext(context, moved);
            }
        }

        @Override
        public void onNext(T item) {
            Objects.requireNonNull(item);
            Object[] moved = threadContext.applyContext(context);
            try {
                subscriber.onNext(item);
            } finally {
                threadContext.restoreContext(context, moved);
            }
        }

        @Override
        public void onError(Throwable failure) {
            Objects.requireNonNull(failure);
            Object[] moved = threadContext.applyContext(context);
            try {
                subscriber.onError(failure);
            } finally {
                threadContext.restoreContext(context, moved);
            }
        }

        @Override
        public void onComplete() {
            Object[] moved = threadContext.applyContext(context);
            try {
                subscriber.onComplete();
            } finally {
                threadContext.restoreContext(context, moved);
            }
        }
    }
}
