-- liquibase formatted sql
-- changeset pwa:init_db_pwa.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_datastore WHERE entity_key LIKE 'pwa.site_property.%'
INSERT INTO 
	core_datastore ( entity_key, entity_value ) 
VALUES
	('pwa.site_property.01.name', ''),
	('pwa.site_property.02.short_name', ''),
	('pwa.site_property.03.description', ''),
	('pwa.site_property.04.display', 'standalone'),
	('pwa.site_property.05.orientation', ''),
	('pwa.site_property.06.start.url', ''),
	('pwa.site_property.07.scope', ''),
	('pwa.site_property.10.background.color', '#ffffff'),
	('pwa.site_property.11.theme.color', '#0000FF'),
	('pwa.site_property.12.categories', '');

