-- liquibase formatted sql
-- changeset pwa:update_db_pwa-1.0.0-2.0.0.sql
-- preconditions onFail:MARK_RAN onError:WARN
DELETE FROM core_datastore WHERE entity_key IN ( 'pwa.site_property.08.fo.start.url', 'pwa.site_property.09.icon.name' );

-- changeset pwa:update_db_pwa-1.0.0-2.0.0-rev1.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_datastore WHERE entity_key = 'pwa.site_property.12.categories'
INSERT INTO core_datastore ( entity_key, entity_value ) VALUES ( 'pwa.site_property.12.categories', '' );

-- changeset pwa:update_db_pwa-1.0.0-2.0.0-rev2.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_admin_right WHERE id_right = 'PWA_MANAGEMENT'
INSERT INTO core_admin_right ( id_right, name, level_right, admin_url, description, is_updatable, plugin_name, id_feature_group, icon_url, documentation_url, id_order ) VALUES
( 'PWA_MANAGEMENT', 'pwa.adminFeature.managePwa.name', 2, 'jsp/admin/plugins/pwa/ManagePwa.jsp', 'pwa.adminFeature.managePwa.description', 0, 'pwa', 'SITE', 'ti ti-device-mobile-cog', NULL, 10 );

-- changeset pwa:update_db_pwa-1.0.0-2.0.0-rev3.sql
-- preconditions onFail:MARK_RAN onError:WARN
-- precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM core_user_right WHERE id_right = 'PWA_MANAGEMENT' AND id_user = 1
INSERT INTO core_user_right ( id_right, id_user ) VALUES ( 'PWA_MANAGEMENT', 1 );
