use sys;

Create TABLE if not exists accounts(
	dbId int NOT NULL AUTO_INCREMENT PRIMARY KEY,
        mail varchar(100) NOT NULL COLLATE utf8_bin,
        name varchar(80) Binary NOT NULL UNIQUE,
        password char(40) Binary NOT NULL
);

CREATE TABLE if not exists contacts(
        dbId int NOT NULL AUTO_INCREMENT PRIMARY KEY,
        accId int not null,	
			FOREIGN KEY fk_account(accId)
			REFERENCES Accounts(dbId)
			ON UPDATE CASCADE
			ON DELETE RESTRICT,
        firstname varchar(100),
        lastname varchar(100),
        city varchar(100),
        zipCode varchar(30)
);

Create Table if not exists workerjobs(
		dbId int NOT NULL AUTO_INCREMENT PRIMARY KEY,
		remitter int NOT NULL,
			FOREIGN KEY fk_account(remitter)
			REFERENCES Accounts(dbId)
			ON UPDATE CASCADE
			ON DELETE RESTRICT,
		operation varchar(50) NOT NULL,
		arguments varchar(2048) NOT NULL,
		mail varchar(50) NOT NULL,
		lockedUntil datetime NOT NULL default now(),
		creationTime datetime NOT NULL default now(),
		doneTime datetime(0)
);

insert into accounts(mail, name, password)
values('tester@test.it', 'Tester', 'Testpw');