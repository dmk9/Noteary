const request = require('supertest');
const app = require('../index'); // Make sure your Express app is exported from index.js

describe('User API', () => {
  let token = '';

  it('should register a new user', async () => {
    const res = await request(app)
      .post('/users/register')
      .send({ name: 'TestUser', email: 'testuser@example.com', pass: 'testpass' });
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty('msg');
  });

  it('should login and return a token', async () => {
    const res = await request(app)
      .post('/users/login')
      .send({ email: 'testuser@example.com', pass: 'testpass' });
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty('token');
    token = res.body.token;
  });

  it('should not login with wrong password', async () => {
    const res = await request(app)
      .post('/users/login')
      .send({ email: 'testuser@example.com', pass: 'wrongpass' });
    expect(res.statusCode).toBe(401);
  });
});

describe('Notes API', () => {
  let token = '';
  let noteId = '';

  beforeAll(async () => {
    // Login to get token
    const res = await request(app)
      .post('/users/login')
      .send({ email: 'testuser@example.com', pass: 'testpass' });
    token = res.body.token;
  });

  it('should create a note', async () => {
    const res = await request(app)
      .post('/notes/create')
      .set('Authorization', token)
      .send({ title: 'Test Note', body: 'This is a test note.' });
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty('msg');
    noteId = res.body.note?._id;
  });

  it('should fetch notes', async () => {
    const res = await request(app)
      .get('/notes')
      .set('Authorization', token);
    expect(res.statusCode).toBe(200);
    expect(Array.isArray(res.body)).toBe(true);
  });

  it('should not create a note without token', async () => {
    const res = await request(app)
      .post('/notes/create')
      .send({ title: 'No Token', body: 'Should fail.' });
    expect(res.statusCode).toBe(401);
});
});