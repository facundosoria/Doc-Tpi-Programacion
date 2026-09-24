-- PAR-14 · Expiración de calibraciones por nueva versión de rúbrica / golden set.
--
-- V27 agregó el estado 'EXPIRED' al enum calibration_state, pero la invariante de V2
-- (CHECK autonombrado calibration_runs_check) siguió tratando como terminales solo
-- PASSED/FAILED/CANCELLED. Como una corrida expirada conserva finished_at (ya había
-- terminado), el UPDATE de CalibrationRunRepository.expire() pasaba a violar el CHECK
-- y publicar una nueva versión de rúbrica con una calibración activa terminaba en 422.
-- La invariante se reemplaza incluyendo 'EXPIRED' en el conjunto de estados terminales.

ALTER TABLE calibration_runs DROP CONSTRAINT calibration_runs_check;
ALTER TABLE calibration_runs ADD CONSTRAINT calibration_runs_check CHECK (
  (state IN ('PASSED', 'FAILED', 'CANCELLED', 'EXPIRED')) = (finished_at IS NOT NULL)
);