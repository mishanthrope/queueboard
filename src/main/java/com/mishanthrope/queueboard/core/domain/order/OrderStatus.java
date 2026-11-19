package com.mishanthrope.queueboard.core.domain.order;

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
        throw new IllegalStatusTransitionError("start", this);
    }

    public OrderStatus markReady() {
        throw new IllegalStatusTransitionError("mark as ready", this);
    }

    public OrderStatus issue() {
        throw new IllegalStatusTransitionError("issue", this);
    }

    public OrderStatus cancel() {
        throw new IllegalStatusTransitionError("cancel", this);
    }

    public OrderStatus restore() {
        throw new IllegalStatusTransitionError("restore", this);
    }

    public boolean isActive() {
        return this == NEW || this == COOKING || this == READY;
    }

    public boolean isInQueue() {
        return this == NEW || this == COOKING;
    }

    public boolean isFinished() {
        return this == ISSUED || this == CANCELLED;
    }
}
