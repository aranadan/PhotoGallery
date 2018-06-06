package com.fox.andrey.photogallery;

        import android.content.Context;
        import android.preference.PreferenceManager;

public class QueryPreferences {
    //константa для запроса
    private static final String PREF_SEARCH_QUERY = "searchQuery";
    //константa для хранения идентификатора последней загруженной фотографии
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    // константa для хранения состояния сигнала
    private static final String PREF_IS_ALARM_ON = "isAlarmOn";


    /*Получение ранее сохраненного значения сводится к простому вызову
SharedPreferences.getString(…), getInt(…) или другого метода, соответствующего типу данных. Второй параметр SharedPreferences.getString(PREF_SEARCH_
QUERY, null) определяет возвращаемое значение по умолчанию, которое должно
возвращаться при отсутствии записи с ключом PREF_SEARCH_QUERY*/
    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY, null);
    }

    /*Метод setStoredQuery(Context) записывает запрос в хранилище общих настроек для заданного контекста. В приведенном выше коде вызов SharedPreferences.
edit() используется для получения экземпляра SharedPreferences.Editor. Этот
класс используется для сохранения значений в SharedPreferences. Он позволяет объединять изменения в транзакции, по аналогии с тем, как это делается
в FragmentTransaction. Множественные изменения могут быть сгруппированы
в одну операцию записи в хранилище.
После того как все изменения будут внесены, вызовите apply() для объекта
Editor, чтобы эти изменения стали видимыми для всех пользователей файла
SharedPreferences. Метод apply() вносит изменения в память немедленно, а непосредственная запись в файл осуществляется в фоновом потоке.*/
    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SEARCH_QUERY, query).apply();
    }

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }
    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, lastResultId)
                .apply();
    }

    public static boolean isAlarmOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON, false);
    }
    public static void setAlarmOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON, isOn)
                .apply();
    }

}
