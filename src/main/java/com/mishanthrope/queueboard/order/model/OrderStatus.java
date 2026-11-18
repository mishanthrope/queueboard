package com.mishanthrope.queueboard.order.model;

import com.mishanthrope.queueboard.order.exception.IllegalStatusTransitionException;

public enum OrderStatus {

    NEW {
        @Override
        public OrderStatus startCooking() {
            return COOKING;
        }

        @Override
        public OrderStatus cancel() {
            return CANCELLED;
        }
    },

    COOKING {
        @Override
        public OrderStatus markReady() {
            return READY;
        }

        @Override
        public OrderStatus cancel() {
            return CANCELLED;
        }
    },

    READY {
        @Override
        public OrderStatus issue() {
            return ISSUED;
        }

        @Override
        public OrderStatus cancel() {
            return CANCELLED;
        }

        @Override
        public OrderStatus restore() {
            return COOKING;
        }
    },

    ISSUED,

    CANCELLED {
        @Override
        public OrderStatus restore() {
            return COOKING;
        }
    };

    public OrderStatus startCooking() {
        throw new IllegalStatusTransitionException("start", this);
    }

    public OrderStatus markReady() {
        throw new IllegalStatusTransitionException("mark as ready", this);
    }

    public OrderStatus issue() {
        throw new IllegalStatusTransitionException("issue", this);
    }

    public OrderStatus cancel() {
        throw new IllegalStatusTransitionException("cancel", this);
    }

    public OrderStatus restore() {
        throw new IllegalStatusTransitionException("restore", this);
    }
}
