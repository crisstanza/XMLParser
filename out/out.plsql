DECLARE
g MDSYS.sdo_geometry;
BEGIN
g := MDSYS.SDO_GEOMETRY(2001,8307,NULL,sdo_elem_info_array(1,1003,1), sdo_ordinate_array(-51.5360,-30.4219,-51.5360,-30.4215));
UPDATE local SET geometria = g WHERE identificacao = '1';
COMMIT;
g := MDSYS.SDO_GEOMETRY(2002,8307,NULL,sdo_elem_info_array(1,1003,1), sdo_ordinate_array(-51.54,-30.43,-51.54,-30.41));
UPDATE local SET geometria = g WHERE identificacao = '2';
COMMIT;
END;
/
