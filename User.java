public class User {
    private int id;
    private String username;
    private String password;
    private String name;
    private int age;
    private String securityQuestion;
    private String securityAnswer;

    public User(int id, String username, String password, String name, int age, String securityQuestion, String securityAnswer) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
        this.securityQuestion = securityQuestion;
        this.securityAnswer = securityAnswer;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSecurityQuestion() {
        return securityQuestion;
    }

    public String getSecurityAnswer() {
        return securityAnswer;
    }

    @Override
    public String toString() {
        return "User Profile [ID=" + id + ", Username=" + username + ", Name=" + name + ", Age=" + age + "]";
    }
}
