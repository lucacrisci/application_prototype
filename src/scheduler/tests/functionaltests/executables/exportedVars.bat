ECHO User var 1 = %USER_VAR_1%
IF NOT "%USER_VAR_1%" == ".User Value 1." GOTO EXIT1

ECHO User var 2 = %USER_VAR_2%
IF NOT "%USER_VAR_2%" == ".User Value 2." GOTO EXIT1


ECHO TID %PAS_TASK_ID%
ECHO TIN %PAS_TASK_NAME%
ECHO JID %PAS_JOB_ID%
ECHO JIN %PAS_JOB_NAME%
GOTO EXITO

:EXIT1
exit 1

:EXITO

