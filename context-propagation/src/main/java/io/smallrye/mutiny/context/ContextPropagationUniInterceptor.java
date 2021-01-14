package io.smallrye.mutiny.context;

import io.smallrye.context.SmallRyeThreadContext;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.UniInterceptor;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * Provides context propagation to Uni types.
 * Subclasses need to override this to provide the Context Propagation ThreadContext.
 */
public abstract class ContextPropagationUniInterceptor implements UniInterceptor {

    @Override
    public <T> UniSubscriber<? super T> onSubscription(Uni<T> instance, UniSubscriber<? super T> subscriber) {
        SmallRyeThreadContext threadContext = getThreadContext();
        Object[] context = threadContext.captureContext();
        return new UniSubscriber<T>() {

            @Override
            public void onSubscribe(UniSubscription subscription) {
                Object[] moved = threadContext.applyContext(context);
                try {
                    subscriber.onSubscribe(subscription);
                } finally {
                    threadContext.restoreContext(context, moved);
                }
            }

            @Override
            public void onItem(T item) {
                Object[] moved = threadContext.applyContext(context);
                try {
                    subscriber.onItem(item);
                } finally {
                    threadContext.restoreContext(context, moved);
                }
            }

            @Override
            public void onFailure(Throwable failure) {
                Object[] moved = threadContext.applyContext(context);
                try {
                    subscriber.onFailure(failure);
                } finally {
                    threadContext.restoreContext(context, moved);
                }
            }
        };
    }

    @Override
    public <T> Uni<T> onUniCreation(Uni<T> uni) {
        SmallRyeThreadContext threadContext = getThreadContext();
        Object[] context = threadContext.captureContext();
        return new AbstractUni<T>() {
            @Override
            protected void subscribing(UniSubscriber<? super T> subscriber) {
                Object[] moved = threadContext.applyContext(context);
                try {
                    AbstractUni.subscribe(uni, subscriber);
                } finally {
                    threadContext.restoreContext(context, moved);
                }
            }
        };
    }

    /**
     * Gets the Context Propagation ThreadContext. External
     * implementations may implement this method.
     *
     * @return the ThreadContext
     * @see DefaultContextPropagationUniInterceptor#getThreadContext()
     */
    protected abstract SmallRyeThreadContext getThreadContext();
}
