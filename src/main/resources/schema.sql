DROP TABLE IF EXISTS top_gainers;

CREATE TABLE top_gainers (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	change_amount DECIMAL(6,4),
	ticker VARCHAR(5),
	change_percentage DECIMAL(8,4),
	price DECIMAL(6,4)
);

DROP TABLE IF EXISTS top_losers;

CREATE TABLE top_losers (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	change_amount DECIMAL(7,4),
	ticker VARCHAR(5),
	change_percentage DECIMAL(8,4),
	price DECIMAL(6,4)
);

DROP TABLE IF EXISTS most_actively_traded;

CREATE TABLE most_actively_traded (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	volume DECIMAL(9,0),
	change_amount DECIMAL(6,4),
	ticker VARCHAR(4),
	change_percentage DECIMAL(8,4),
	price DECIMAL(7,4)
);

