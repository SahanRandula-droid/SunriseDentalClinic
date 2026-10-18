DROP DATABASE IF EXISTS sunrise_dental_test;
CREATE DATABASE sunrise_dental_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sunrise_dental_test;

CREATE TABLE users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(30)  NOT NULL UNIQUE,
    password_hash  CHAR(64)     NOT NULL,
    password_salt  CHAR(32)     NOT NULL,
    full_name      VARCHAR(100) NOT NULL,
    role           ENUM('ADMIN', 'RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    active         TINYINT(1)   NOT NULL DEFAULT 1,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dentists (
    dentist_id        INT AUTO_INCREMENT PRIMARY KEY,
    dentist_name      VARCHAR(100)  NOT NULL,
    specialization    VARCHAR(100)  NOT NULL,
    consultation_fee  DECIMAL(10,2) NOT NULL,
    active            TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT chk_dentist_fee CHECK (consultation_fee >= 0)
);

CREATE TABLE treatments (
    treatment_id    INT AUTO_INCREMENT PRIMARY KEY,
    treatment_name  VARCHAR(100)  NOT NULL UNIQUE,
    treatment_cost  DECIMAL(10,2) NOT NULL,
    active          TINYINT(1)    NOT NULL DEFAULT 1,
    CONSTRAINT chk_treatment_cost CHECK (treatment_cost >= 0)
);

CREATE TABLE patients (
    patient_id      INT AUTO_INCREMENT PRIMARY KEY,
    patient_name    VARCHAR(100) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    contact_number  CHAR(10)     NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_patient UNIQUE (patient_name, contact_number)
);

CREATE TABLE appointment_sequence (
    seq_year     INT PRIMARY KEY,
    last_number  INT NOT NULL
);

CREATE TABLE appointments (
    appointment_id    INT AUTO_INCREMENT PRIMARY KEY,
    appointment_no    VARCHAR(15) NOT NULL UNIQUE,
    patient_id        INT  NOT NULL,
    dentist_id        INT  NOT NULL,
    treatment_id      INT  NOT NULL,
    appointment_date  DATE NOT NULL,
    appointment_time  TIME NOT NULL,
    status            ENUM('BOOKED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'BOOKED',
    created_by        INT  NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    slot_key          VARCHAR(40) GENERATED ALWAYS AS (
                          IF(status = 'CANCELLED', NULL,
                             CONCAT(dentist_id, '|',
                                    DATE_FORMAT(appointment_date, '%Y%m%d'), '|',
                                    TIME_FORMAT(appointment_time, '%H%i')))
                      ) STORED,
    CONSTRAINT fk_app_patient   FOREIGN KEY (patient_id)   REFERENCES patients(patient_id),
    CONSTRAINT fk_app_dentist   FOREIGN KEY (dentist_id)   REFERENCES dentists(dentist_id),
    CONSTRAINT fk_app_treatment FOREIGN KEY (treatment_id) REFERENCES treatments(treatment_id),
    CONSTRAINT fk_app_user      FOREIGN KEY (created_by)   REFERENCES users(user_id),
    CONSTRAINT uq_active_slot   UNIQUE (slot_key),
    INDEX idx_app_date (appointment_date)
);

CREATE TABLE bills (
    bill_id           INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id    INT NOT NULL UNIQUE,
    treatment_cost    DECIMAL(10,2) NOT NULL,
    consultation_fee  DECIMAL(10,2) NOT NULL,
    discount_type     VARCHAR(20)   NOT NULL DEFAULT 'NONE',
    discount_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount      DECIMAL(10,2) NOT NULL,
    generated_by      INT NOT NULL,
    generated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_app  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id),
    CONSTRAINT fk_bill_user FOREIGN KEY (generated_by)   REFERENCES users(user_id),

    CONSTRAINT chk_bill_total CHECK (total_amount = treatment_cost + consultation_fee - discount_amount),
    CONSTRAINT chk_bill_discount CHECK (discount_amount >= 0)
);

CREATE TABLE notifications (
    notification_id  INT AUTO_INCREMENT PRIMARY KEY,
    appointment_no   VARCHAR(15)  NOT NULL,
    recipient        VARCHAR(20)  NOT NULL,
    message          VARCHAR(500) NOT NULL,
    status           ENUM('PENDING', 'SENT') NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_log (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id  INT NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    details         VARCHAR(255) NOT NULL,
    logged_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE VIEW vw_appointment_details AS
SELECT a.appointment_id,
       a.appointment_no,
       a.appointment_date,
       a.appointment_time,
       a.status,
       a.created_at,
       p.patient_id,
       p.patient_name,
       p.address,
       p.contact_number,
       d.dentist_id,
       d.dentist_name,
       d.specialization,
       d.consultation_fee,
       t.treatment_id,
       t.treatment_name,
       t.treatment_cost,
       u.username AS created_by_username
FROM appointments a
JOIN patients   p ON p.patient_id   = a.patient_id
JOIN dentists   d ON d.dentist_id   = a.dentist_id
JOIN treatments t ON t.treatment_id = a.treatment_id
JOIN users      u ON u.user_id      = a.created_by;

DELIMITER $$

CREATE FUNCTION fn_format_appointment_no(p_year INT, p_number INT)
RETURNS VARCHAR(15)
DETERMINISTIC
NO SQL
BEGIN
    RETURN CONCAT('APT-', p_year, '-', LPAD(p_number, 4, '0'));
END$$

CREATE PROCEDURE sp_register_appointment(
    IN  p_patient_name    VARCHAR(100),
    IN  p_address         VARCHAR(255),
    IN  p_contact_number  CHAR(10),
    IN  p_dentist_id      INT,
    IN  p_treatment_id    INT,
    IN  p_date            DATE,
    IN  p_time            TIME,
    IN  p_user_id         INT,
    OUT p_appointment_no  VARCHAR(15)
)
BEGIN
    DECLARE v_patient_id INT DEFAULT NULL;
    DECLARE v_year       INT DEFAULT YEAR(CURDATE());
    DECLARE v_number     INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    SELECT patient_id INTO v_patient_id
    FROM patients
    WHERE patient_name = p_patient_name AND contact_number = p_contact_number
    LIMIT 1;

    IF v_patient_id IS NULL THEN
        INSERT INTO patients (patient_name, address, contact_number)
        VALUES (p_patient_name, p_address, p_contact_number);
        SET v_patient_id = LAST_INSERT_ID();
    ELSE
        UPDATE patients SET address = p_address WHERE patient_id = v_patient_id;
    END IF;

    INSERT INTO appointment_sequence (seq_year, last_number)
    VALUES (v_year, 1)
    ON DUPLICATE KEY UPDATE last_number = last_number + 1;

    SELECT last_number INTO v_number
    FROM appointment_sequence
    WHERE seq_year = v_year;

    SET p_appointment_no = fn_format_appointment_no(v_year, v_number);

    INSERT INTO appointments (appointment_no, patient_id, dentist_id, treatment_id,
                              appointment_date, appointment_time, created_by)
    VALUES (p_appointment_no, v_patient_id, p_dentist_id, p_treatment_id,
            p_date, p_time, p_user_id);

    COMMIT;
END$$

CREATE TRIGGER trg_appointments_before_insert
BEFORE INSERT ON appointments
FOR EACH ROW
BEGIN
    IF EXISTS (SELECT 1 FROM appointments
               WHERE dentist_id = NEW.dentist_id
                 AND appointment_date = NEW.appointment_date
                 AND appointment_time = NEW.appointment_time
                 AND status <> 'CANCELLED') THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'The dentist already has an appointment at this date and time.';
    END IF;
END$$

CREATE TRIGGER trg_appointments_before_update
BEFORE UPDATE ON appointments
FOR EACH ROW
BEGIN
    IF OLD.status <> 'BOOKED' AND NEW.status <> OLD.status THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Only a BOOKED appointment can change its status.';
    END IF;
END$$

CREATE TRIGGER trg_appointments_after_insert
AFTER INSERT ON appointments
FOR EACH ROW
BEGIN
    INSERT INTO audit_log (appointment_id, action, details)
    VALUES (NEW.appointment_id, 'CREATED',
            CONCAT('Appointment ', NEW.appointment_no, ' booked for ',
                   NEW.appointment_date, ' ', NEW.appointment_time));
END$$

CREATE TRIGGER trg_appointments_after_update
AFTER UPDATE ON appointments
FOR EACH ROW
BEGIN
    IF NEW.status <> OLD.status THEN
        INSERT INTO audit_log (appointment_id, action, details)
        VALUES (NEW.appointment_id, 'STATUS_CHANGED',
                CONCAT('Status changed from ', OLD.status, ' to ', NEW.status));
    END IF;
END$$

CREATE TRIGGER trg_bills_after_insert
AFTER INSERT ON bills
FOR EACH ROW
BEGIN
    UPDATE appointments
    SET status = 'COMPLETED'
    WHERE appointment_id = NEW.appointment_id AND status = 'BOOKED';
END$$

DELIMITER ;

INSERT INTO users (username, password_hash, password_salt, full_name, role) VALUES
('admin',     'd31f5274ff5fe6b9b51ef3b61b71741d09ef2abc33e66c5679f59a698d43dc53', '37b011bd15fcefe14fe42ca1dfc69e35', 'Clinic Manager',     'ADMIN'),
('reception', 'dfcb728b5e23ba190b4ea6646e495b3efb8afa3b8f7f0d97148dd026db877a07', '9d796e154d41f64df21ed1cda8b53fd4', 'Front Desk Officer', 'RECEPTIONIST');

INSERT INTO dentists (dentist_name, specialization, consultation_fee) VALUES
('Dr. Nimal Perera',      'General Dentistry',  1500.00),
('Dr. Shanika Fernando',  'Orthodontics',       2500.00),
('Dr. Ruwan Jayasinghe',  'Oral Surgery',       3000.00),
('Dr. Ayesha Silva',      'Paediatric Dentistry', 2000.00);

INSERT INTO treatments (treatment_name, treatment_cost) VALUES
('Dental Check-up',        1000.00),
('Scaling and Polishing',  3500.00),
('Tooth Filling',          4500.00),
('Tooth Extraction',       4000.00),
('Root Canal Treatment',  18000.00),
('Teeth Whitening',       15000.00),
('Braces Consultation',    2000.00),
('Dental Crown',          25000.00);
