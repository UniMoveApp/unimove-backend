CREATE TABLE messages (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    content VARCHAR(1000) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_messages_ride FOREIGN KEY(ride_id) REFERENCES rides(id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_ride ON messages(ride_id);
