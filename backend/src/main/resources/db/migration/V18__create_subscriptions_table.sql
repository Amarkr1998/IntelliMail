-- One subscription per organization, auto-created (status=TRIALING) the
-- moment an organization is created. stripe_customer_id stays NULL until the
-- first real checkout - no Stripe API call happens just from creating an org.
CREATE TABLE subscriptions (
    id                      UUID          PRIMARY KEY,
    organization_id         UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    stripe_customer_id      VARCHAR(255),
    stripe_subscription_id  VARCHAR(255),
    plan_id                 VARCHAR(20)   NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    trial_ends_at           TIMESTAMPTZ,
    current_period_end      TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL,
    updated_at              TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_subscriptions_organization_id UNIQUE (organization_id),
    CONSTRAINT uk_subscriptions_stripe_subscription_id UNIQUE (stripe_subscription_id)
);

CREATE INDEX idx_subscriptions_stripe_customer_id ON subscriptions (stripe_customer_id);
CREATE INDEX idx_subscriptions_stripe_subscription_id ON subscriptions (stripe_subscription_id);
